// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006-2007 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.commons.ui.command;

import org.eclipse.gef.commands.CommandStack;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Widget;

/**
 * DOC amaumont class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class CommandStackForComposite extends CommandStack {

    private Control refComposite;

    private Listener keyListener;

    private boolean refControlIsShell;

    private static final int KEY_CODE_REDO = 121; // 'y'

    private static final int KEY_CODE_UNDO = 122; // 'z'

    /**
     * DOC amaumont IndependantCommandStackHandler constructor comment.
     */
    public CommandStackForComposite(Composite refComposite) {
        super();
        this.refComposite = refComposite;
        refControlIsShell = refComposite instanceof Shell;
        init();
    }

    public void init() {

        keyListener = new Listener() {

            public void handleEvent(Event event) {
                Widget widget = event.widget;
                if (widget instanceof Control) {
                    Control control = (Control) widget;
                    if (refControlIsShell && control.getShell() == refComposite || !refControlIsShell && refControlIsParentOf(control)) {
                        keyPressedExecute(event);
                    }
                }
            }

        };
        this.refComposite.getDisplay().addFilter(SWT.KeyDown, keyListener);
    }

    /**
     * DOC amaumont Comment method "refControlIsParentOf".
     * 
     * @param control
     * @return
     */
    protected boolean refControlIsParentOf(Control control) {

        while (true) {
            Composite parentComposite = control.getParent();
            if (parentComposite == refComposite) {
                return true;
            } else if (parentComposite == null) {
                return false;
            }
            control = parentComposite;
        }

    }

    /**
     * DOC amaumont Comment method "keyPressedExecute".
     * 
     * @param e
     */
    protected void keyPressedExecute(Event e) {
        boolean ctrlKey = (e.stateMask & (SWT.CTRL)) != 0;
        boolean undo = ctrlKey && e.keyCode == KEY_CODE_UNDO;
        boolean redo = ctrlKey && e.keyCode == KEY_CODE_REDO;
        if (redo) {
            if (canRedo()) {
                redo();
            }
        } else if (undo) {
            if (canUndo()) {
                undo();
            }
        }
    }

}
