/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package test.com.sun.max.vm.bytecode;

import java.io.*;
import java.util.*;

import junit.framework.*;
import test.com.sun.max.vm.compiler.*;

import com.sun.max.collect.*;
import com.sun.max.program.*;
import com.sun.max.vm.actor.member.*;
import com.sun.max.vm.bytecode.*;
import com.sun.max.vm.bytecode.refmaps.*;
import com.sun.max.vm.classfile.*;
import com.sun.max.vm.classfile.constant.*;
import com.sun.max.vm.compiler.bir.*;
import com.sun.max.vm.type.*;
import com.sun.max.vm.verifier.*;
import com.sun.max.vm.verifier.types.*;

/**
 * Tests the {@linkplain ReferenceMapInterpreter abstract interpreter} used to compute reference maps. The approach
 * taken is to generate a pair of reference maps for every bytecode instruction in a method. The first map is generated
 * with the abstract interpreter and the second is generated by a specialized version of the
 * {@linkplain TypeInferencingMethodVerifier type inferencing} or {@linkplain TypeCheckingMethodVerifier type checking}
 * verifier. The test succeeds for a given method if the two maps in each pair
 * {@linkplain ReferenceMap#equals(Object) match} each other.
 *
 * @author Doug Simon
 */
@org.junit.runner.RunWith(org.junit.runners.AllTests.class)
public class ReferenceMapInterpreterTest extends CompilerTestCase<BirMethod> {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(ReferenceMapInterpreterTest.suite());
    }

    Sequence<String> readClassNames(File file) throws IOException {
        final AppendableSequence<String> lines = new ArrayListSequence<String>();
        final BufferedReader reader = new BufferedReader(new FileReader(file));
        String line;
        while ((line = reader.readLine()) != null) {
            final String className = line.trim();
            if (!line.startsWith("#") && line.length() > 0) {
                lines.append(className);
            }
        }
        return lines;
    }
    public static Test suite() {
        final TestSuite suite = new TestSuite(ReferenceMapInterpreterTest.class.getSimpleName());
        suite.addTestSuite(ReferenceMapInterpreterTest.class);
        return new BirCompilerTestSetup(suite);
    }

    /**
     * Describes which local variable indices and operand stack indices contain references.
     */
    static class ReferenceMap {
        final boolean[] _locals;
        final boolean[] _stack;

        ReferenceMap(int maxLocals, int maxStack) {
            _locals = new boolean[maxLocals];
            _stack = new boolean[maxStack];
        }

        /**
         * @see ReferenceMapInterpreter#isReference(boolean, byte[], ConstantPool, VerificationType)
         */
        private void init(boolean receiverTypeIsWord, byte[] code, ConstantPool constantPool, boolean[] referenceMap, VerificationType[] typeMap) {
            for (int i = 0; i != typeMap.length; ++i) {
                if (ReferenceMapInterpreter.isReference(receiverTypeIsWord, code, constantPool, typeMap[i])) {
                    referenceMap[i] = true;
                }
            }
        }

        ReferenceMap(boolean receiverTypeIsWord, byte[] code, ConstantPool constantPool, Frame frame, int maxLocals, int maxStack) {
            this(maxLocals, maxStack);
            init(receiverTypeIsWord, code, constantPool, _locals, frame.locals());
            init(receiverTypeIsWord, code, constantPool, _stack, frame.stack());
        }

        /**
         * Determines if this reference map denotes references at the same local variable and operand stack indices as a
         * given reference map.
         */
        @Override
        public boolean equals(Object other) {
            if (other instanceof ReferenceMap) {
                final ReferenceMap otherReferenceMap = (ReferenceMap) other;
                return Arrays.equals(_stack, otherReferenceMap._stack) && Arrays.equals(_locals, otherReferenceMap._locals);
            }
            return false;
        }

        @Override
        public String toString() {
            return "stack = " + Arrays.toString(_stack) + ", locals = " + Arrays.toString(_locals);
        }
    }

    /**
     * Computes a {@linkplain ReferenceMap reference map} for each bytecode position in a method using the
     * {@linkplain ReferenceMapInterpreter abstract interpreter}.
     */
    static class InterpreterMapMaker implements ReferenceMapInterpreterContext, ReferenceSlotVisitor {

        private final ClassMethodActor _classMethodActor;
        private final Object _blockFrames;
        private final BirBlock[] _blocks;
        private final ExceptionHandler[] _exceptionHandlerMap;
        private final ReferenceMap[] _referenceMaps;
        private final BytecodePositionIterator _bytecodePositionIterator;

        InterpreterMapMaker(BirMethod birMethod) {
            _classMethodActor = birMethod.classMethodActor();
            _blocks = Sequence.Static.toArray(birMethod.blocks(), BirBlock.class);
            _blockFrames = ReferenceMapInterpreter.createFrames(this);
            final CodeAttribute codeAttribute = _classMethodActor.codeAttribute();
            _exceptionHandlerMap = ExceptionHandler.createHandlerMap(codeAttribute);
            _referenceMaps = new ReferenceMap[codeAttribute.code().length];

            final AppendableIndexedSequence<Integer> bytecodePositions = new ArrayListSequence<Integer>();
            final BytecodeAdapter bytecodeAdapter = new BytecodeAdapter() {
                @Override
                protected void opcodeDecoded() {
                    final int bytecodePosition = bytecodeScanner().currentOpcodePosition();
                    bytecodePositions.append(bytecodePosition);
                    _referenceMaps[bytecodePosition] = new ReferenceMap(codeAttribute.maxLocals(), codeAttribute.maxStack());
                }
            };
            new BytecodeScanner(bytecodeAdapter).scan(_classMethodActor);

            _bytecodePositionIterator = new BytecodePositionIterator() {
                private int _index;
                public int bytecodePosition() {
                    if (_index < bytecodePositions.length()) {
                        return bytecodePositions.get(_index);
                    }
                    return -1;
                }
                public int next() {
                    if (++_index < bytecodePositions.length()) {
                        return bytecodePositions.get(_index);
                    }
                    return -1;
                }
                public void reset() {
                    _index = 0;
                }
            };
        }

        public Object blockFrames() {
            return _blockFrames;
        }

        public int blockIndexFor(int bytecodePosition) {
            for (int blockIndex = 0; blockIndex < _blocks.length; ++blockIndex) {
                final BirBlock block = _blocks[blockIndex];
                if (block.bytecodeBlock().start() > bytecodePosition) {
                    assert blockIndex > 0;
                    return blockIndex - 1;
                }
            }
            return _blocks.length - 1;
        }

        public int blockStartBytecodePosition(int blockIndex) {
            if (blockIndex == _blocks.length) {
                return classMethodActor().codeAttribute().code().length;
            }
            return _blocks[blockIndex].bytecodeBlock().start();
        }

        public ClassMethodActor classMethodActor() {
            return _classMethodActor;
        }

        public ExceptionHandler exceptionHandlersActiveAt(int bytecodePosition) {
            if (_exceptionHandlerMap == null) {
                return null;
            }
            return _exceptionHandlerMap[bytecodePosition];
        }

        public int numberOfBlocks() {
            return _blocks.length;
        }

        public void visitReferenceInLocalVariable(int localVariableIndex) {
            final ReferenceMap map = _referenceMaps[_bytecodePositionIterator.bytecodePosition()];
            map._locals[localVariableIndex] = true;
        }

        public void visitReferenceOnOperandStack(int operandStackIndex, boolean parametersPopped) {
            if (!parametersPopped) {
                final ReferenceMap map = _referenceMaps[_bytecodePositionIterator.bytecodePosition()];
                map._stack[operandStackIndex] = true;
            }
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + "[" + classMethodActor() + "]";
        }

        public ReferenceMap[] maps() {
            final ReferenceMapInterpreter interpreter = ReferenceMapInterpreter.from(_blockFrames);
            interpreter.finalizeFrames(this);
            interpreter.interpretReferenceSlots(this, this, _bytecodePositionIterator);
            return _referenceMaps;
        }

        public String[] framesAsStrings() {
            return ReferenceMapInterpreter.from(_blockFrames).framesToStrings(this);
        }
    }

    private ClassVerifier _classVerifier;

    /**
     * Deletes frames derived from a {@link StackMapTable} attribute that do not align with the start of a basic block.
     * This situation rarely occurs but when it does, the recorded frame is usually more "exact" that what the abstract
     * interpreter will infer. In particular, a StackMapTable may state that a given local variable or operand stack
     * slot is undefined even though every control flow path to the associated bytecode position defines the
     * variable/slot. The reason it can be correctly denoted as undefined is that there are no subsequent uses of the
     * variable/slot. That is, the variable/slot is no longer live. For example:
     * <p>
     *
     * <pre>
     *  1:  int foo(int a) {
     *  2:      int b = 4;
     *  3:      a = a + b;
     *  4:      return a;
     *  5:  }
     * </pre>
     *
     * In this code, a valid StackMapTable entry for the bytecode position corresponding to line 4 may state that local
     * variable {@code b} is undefined. However, the abstract interpreter would infer that it still has a valid value at
     * this position.
     *
     * @param frameMap
     * @param blockStarts
     */
    private static void clearNonBlockStartFrames(Frame[] frameMap, boolean[] blockStarts) {
        for (int bytecodePosition = 0; bytecodePosition != frameMap.length; ++bytecodePosition) {
            if (frameMap[bytecodePosition] != null && !blockStarts[bytecodePosition]) {
                frameMap[bytecodePosition] = null;
            }
        }
    }

    @Override
    protected BirMethod compileMethod(final ClassMethodActor classMethodActor) {
        final BirMethod method = super.compileMethod(classMethodActor);

        if (_classVerifier == null || !_classVerifier.classActor().equals(classMethodActor.holder())) {
            _classVerifier = Verifier.verifierFor(classMethodActor.holder());
        }

        final CodeAttribute codeAttribute = classMethodActor.codeAttribute();
        final int maxLocals = codeAttribute.maxLocals();
        final int maxStack = codeAttribute.maxStack();
        final int codeLength = codeAttribute.code().length;

        final InterpreterMapMaker interpreterMapMaker = new InterpreterMapMaker(method);
        final boolean[] blockStarts = new boolean[codeLength];
        for (BirBlock birBlock : method.blocks()) {
            blockStarts[birBlock.bytecodeBlock().start()] = true;
        }

        final ReferenceMap[] verifiedMaps = createReferenceMapsWithVerifier(classMethodActor, codeAttribute, maxLocals, maxStack, codeLength, blockStarts);
        final ReferenceMap[] interpretedMaps = interpreterMapMaker.maps();

        assertEquals(verifiedMaps.length, interpretedMaps.length);
        for (int bytecodePostion = 0; bytecodePostion != verifiedMaps.length; ++bytecodePostion) {
            final ReferenceMap verifiedMap = verifiedMaps[bytecodePostion];
            final ReferenceMap interpretedMap = interpretedMaps[bytecodePostion];
            if (verifiedMap == null) {
                if (interpretedMap != null) {
                    fail(classMethodActor.format("%H.%n(%p)") + "[" + bytecodePostion + "]: interpreter frame but no verifier frame");
                }
            } else if (!verifiedMap.equals(interpretedMap)) {
                fail(makeErrorMessage(classMethodActor, codeAttribute, codeLength, interpreterMapMaker, bytecodePostion, verifiedMap, interpretedMap));
            }
        }
        return method;
    }

    private String makeErrorMessage(final ClassMethodActor classMethodActor, final CodeAttribute codeAttribute, final int codeLength, final InterpreterMapMaker interpreterMapMaker,
                    int bytecodePostion, final ReferenceMap verifiedMap, final ReferenceMap interpretedMap) {
        final StringBuilder annotatedDisassembly = new StringBuilder();
        final String[] framesAsStrings = interpreterMapMaker.framesAsStrings();
        final int numberOfBlocks = interpreterMapMaker.numberOfBlocks();
        for (int blockIndex = 0; blockIndex != numberOfBlocks; ++blockIndex) {
            final int bytecodePosition = interpreterMapMaker.blockStartBytecodePosition(blockIndex);
            if (blockIndex != 0) {
                final BytecodeBlock bytecodeBlock = new BytecodeBlock(codeAttribute.code(), interpreterMapMaker.blockStartBytecodePosition(blockIndex - 1), bytecodePosition - 1);
                final String disassembly = BytecodePrinter.toString(codeAttribute.constantPool(), bytecodeBlock, "        ", "\n", 0);
                annotatedDisassembly.append(disassembly);
            }
            annotatedDisassembly.append(String.format("      %s%n", framesAsStrings[blockIndex]));
            if (blockIndex == numberOfBlocks - 1) {
                final BytecodeBlock bytecodeBlock = new BytecodeBlock(codeAttribute.code(), interpreterMapMaker.blockStartBytecodePosition(blockIndex), codeLength - 1);
                final String disassembly = BytecodePrinter.toString(codeAttribute.constantPool(), bytecodeBlock, "        ", "\n", 0);
                annotatedDisassembly.append(disassembly);
            }
        }

        final CharArrayWriter stackMapTable = new CharArrayWriter();
        CodeAttributePrinter.printStackMapTable(codeAttribute, new PrintWriter(stackMapTable));
        final String errorMessage = String.format("%s[%d]: interpreter and verifier maps don't match%n" +
                        "    interpreter frame: %s%n" +
                        "       verifier frame: %s%n" +
                        "    disassembly with interpreter block frames:%n%s%n" +
                        "    %s", classMethodActor.format("%H.%n(%p)"), bytecodePostion, interpretedMap, verifiedMap, annotatedDisassembly, stackMapTable.toString());
        return errorMessage;
    }

    private ReferenceMap[] createReferenceMapsWithVerifier(
                    final ClassMethodActor classMethodActor,
                    final CodeAttribute codeAttribute,
                    final int maxLocals,
                    final int maxStack,
                    final int codeLength,
                    final boolean[] blockStarts) {
        final ReferenceMap[] verifiedMaps = new ReferenceMap[codeLength];
        if (_classVerifier instanceof TypeCheckingVerifier) {
            new TypeCheckingMethodVerifier(_classVerifier, classMethodActor, codeAttribute) {
                private final boolean _receiverTypeIsWord = thisObjectType.typeDescriptor().toKind() == Kind.WORD;
                @Override
                public void verify() {
                    clearNonBlockStartFrames(frameMap, blockStarts);
                    super.verify();
                }

                @Override
                protected void preInstructionScan() {
                    super.preInstructionScan();
                    final int currentOpcodePosition = currentOpcodePosition();
                    verifiedMaps[currentOpcodePosition] = new ReferenceMap(_receiverTypeIsWord, codeAttribute().code(), constantPool(), frame, maxLocals, maxStack);
                }
            }.verify();
        } else {
            new TypeInferencingMethodVerifier(_classVerifier, classMethodActor, codeAttribute) {
                private final boolean _receiverTypeIsWord = thisObjectType.typeDescriptor().toKind() == Kind.WORD;
                @Override
                public void verify() {
                    clearNonBlockStartFrames(frameMap, blockStarts);
                    super.verify();
                }

                @Override
                protected void preInstructionScan() {
                    super.preInstructionScan();
                    final int currentOpcodePosition = currentOpcodePosition();
                    verifiedMaps[currentOpcodePosition] = new ReferenceMap(_receiverTypeIsWord, codeAttribute().code(), constantPool(), frame, maxLocals, maxStack);
                }
            }.verify();
        }
        return verifiedMaps;
    }

    public ReferenceMapInterpreterTest(String name) {
        super(name);
    }

    public void test_one() {
        compileMethod(test.output.HelloWorld.class, "main");
    }

    private static final File JCK_CLASSES_LIST = new File("test/test/com/sun/max/vm/verifier/jck.classes.txt");

    public void test_positive_jck_classes() {
        if (JCK_CLASSES_LIST.exists()) {
            Trace.line(1, "Running JCK test classes in " + JCK_CLASSES_LIST.getAbsolutePath());
            try {
                for (String className : readClassNames(JCK_CLASSES_LIST)) {
                    if (!className.endsWith("n")) {
                        try {
                            compileClass(Class.forName(className));
                        } catch (ClassNotFoundException classNotFoundException) {
                            ProgramWarning.message("JCK class not found: " + classNotFoundException);
                            break;
                        } catch (LinkageError linkageError) {
                            ProgramWarning.message("Error while testing JCK class " + className + ": " + linkageError);
                            break;
                        }
                    }
                }
            } catch (IOException ioException) {
                ProgramWarning.message("IO error while reading JCK classes from " + JCK_CLASSES_LIST.getAbsolutePath() + ": " + ioException);
            }
        } else {
            ProgramWarning.message("File listing JCK classes does not exist: " + JCK_CLASSES_LIST.getAbsolutePath());
        }
    }
}
