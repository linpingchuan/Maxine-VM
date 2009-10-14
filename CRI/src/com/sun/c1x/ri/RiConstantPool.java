/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
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
package com.sun.c1x.ri;

import com.sun.c1x.ci.CiConstant;

/**
 * This interface represents the runtime representation of the constant pool that is
 * used by the compilers when parsing bytecode. The <code>lookup</code> methods look up a constant
 * pool entry without performing  resolution, and are used during compilation.
 *
 * @author Ben L. Titzer
 */
public interface RiConstantPool {

    /**
     * Resolves a reference to a method for an INVOKEVIRTUAL operation at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod resolveInvokeVirtual(char cpi);

    /**
     * Resolves a reference to a method for an INVOKESPECIAL operation at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod resolveInvokeSpecial(char cpi);

    /**
     * Resolves a reference to a method for an INVOKEINTERFACE operation at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod resolveInvokeInterface(char cpi);

    /**
     * Resolves a reference to a method for an INVOKESTATIC operation at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod resolveInvokeStatic(char cpi);

    /**
     * Resolves a reference to a compiler interface type at runtime.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface type
     */
    RiType resolveType(char cpi);

    /**
     * Looks up a reference to a field for a GETFIELD operation at compile time
     * (does not perform resolution). If a GETFIELD of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface field
     */
    RiField lookupGetField(char cpi);

    /**
     * Looks up a reference to a field for a PUTFIELD operation at compile time
     * (does not perform resolution). If a PUTTFIELD of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface field
     */
    RiField lookupPutField(char cpi);

    /**
     * Looks up a reference to a field for a GETSTATIC operation at compile time
     * (does not perform resolution).  If a GETSTATIC of this constant would fail
     * at run time, the compiler expects this method to return an unresolved static.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface field
     */
    RiField lookupGetStatic(char cpi);

    /**
     * Looks a reference to a field for a PUTSTATIC operation at compile time
     * (does not perform resolution). If a PUTSTATIC of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface field
     */
    RiField lookupPutStatic(char cpi);

    /**
     * Looks up a reference to a method for an INVOKEVIRTUAL operation at compile time
     * (does not perform resolution).  If an INVOKEVIRTUAL of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod lookupInvokeVirtual(char cpi);

    /**
     * Looks up a reference to a method for an INVOKESPECIAL operation at compile time
     * (does not perform resolution). If an INVOKESPECIAL of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod lookupInvokeSpecial(char cpi);

    /**
     * Looks up a reference to a method for an INVOKEINTERFACE operation at compile time
     * (does not perform resolution). If an INVOKEINTERFACE of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod lookupInvokeInterface(char cpi);

    /**
     * Looks up a reference to a method for an INVOKESTATIC operation at compile time
     * (does not perform resolution). If an INVOKESTATIC of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface method
     */
    RiMethod lookupInvokeStatic(char cpi);

    /**
     * Looks up a reference to a compiler interface type at compile time (does not
     * perform resolution). If a resolution of this constant would fail
     * at run time, the compiler expects this method to return an unresolved constant.
     * @param cpi the constant pool index
     * @return a reference to the compiler interface type
     */
    RiType lookupType(char cpi);

    /**
     * Looks up a constant at the specified index.
     * @param cpi the constant pool index
     * @return the {@code CiConstant} instance representing the constant
     */
    Object lookupConstant(char cpi);

    /**
     * Creates an exception handler with the specified properties.
     * @param startBCI the start bytecode index of the protected range
     * @param endBCI the end bytecode index of the protected range
     * @param catchBCI the bytecode index of the catch block
     * @param classCPI the constant pool index of the class of the caught exception
     * @return a new exception handler object
     */
    RiExceptionHandler newExceptionHandler(int startBCI, int endBCI, int catchBCI, int classCPI);

    /**
     * Constant object that can be used to identify this constant pool when it is referenced from the code.
     *
     * @return a constant object representing this constant pool
     */
    CiConstant encoding();
}