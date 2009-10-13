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
package com.sun.max.ins.gui;

import com.sun.max.ins.*;
import com.sun.max.ins.method.*;
import com.sun.max.tele.*;
import com.sun.max.tele.method.*;


/**
 * Visual inspector and debugger for code discovered in the VM that is not compiled Java.
 * That is, it's runtime assembled code such as a {@linkplain RuntimeStub stub} or
 * is other native code about which little is known.
 *
 * @author Michael Van De Vanter
 * @author Doug Simon
 */
public final class NativeMethodInspector extends MethodInspector {

    private final TeleTargetRoutine teleTargetRoutine;
    private TargetCodeViewer targetCodeViewer = null;
    private final String shortName;
    private final String longName;

    public NativeMethodInspector(Inspection inspection, MethodInspectorContainer parent, TeleTargetRoutine teleTargetRoutine) {
        super(inspection, parent, null, teleTargetRoutine.teleRoutine());
        this.teleTargetRoutine = teleTargetRoutine;
        if (teleTargetRoutine instanceof TeleNativeTargetRoutine) {
            final TeleNativeTargetRoutine teleNativeTargetRoutine  = (TeleNativeTargetRoutine) teleTargetRoutine;
            shortName = inspection().nameDisplay().shortName(teleNativeTargetRoutine);
            longName = inspection().nameDisplay().longName(teleNativeTargetRoutine);
        } else {
            shortName = teleTargetRoutine.getName();
            longName = shortName;
        }
        createFrame();
    }

    @Override
    public TeleTargetRoutine teleTargetRoutine() {
        return teleTargetRoutine;
    }

    @Override
    public String getTextForTitle() {
        return shortName;
    }

    @Override
    public String getToolTip() {
        return longName;
    }

    @Override
    public void createView() {
        targetCodeViewer =  new JTableTargetCodeViewer(inspection(), this, teleTargetRoutine);
        frame().getContentPane().add(targetCodeViewer);
        frame().pack();
        frame().invalidate();
        frame().repaint();
    }

    @Override
    protected boolean refreshView(boolean force) {
        if (isShowing() || force) {
            targetCodeViewer.refresh(force);
        }
        return true;
    }

    public void viewConfigurationChanged() {
        targetCodeViewer.redisplay();
    }

    @Override
    public void print() {
        targetCodeViewer.print(getTextForTitle());
    }

    /**
     * Receive request from codeViewer to close; there's only one, so close the whole MethodInspector.
     */
    @Override
    public void closeCodeViewer(CodeViewer codeViewer) {
        assert codeViewer == targetCodeViewer;
        close();
    }

    /**
     * Global code selection has changed; update viewer.
     */
    @Override
    public void codeLocationFocusSet(TeleCodeLocation codeLocation, boolean interactiveForNative) {
        if (targetCodeViewer.updateCodeFocus(codeLocation) && !isSelected()) {
            highlight();
        }
    }
}
