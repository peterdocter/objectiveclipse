/*******************************************************************************
 * Copyright (c) 2009 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Ericsson - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.mi.service.command.commands;

import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

/** 
 * Step backwards i instructions, entering function calls. 
 * 
 * @since 2.0
 */
public class MIExecReverseStepInstruction extends MICommand<MIInfo> {

    public MIExecReverseStepInstruction(IExecutionDMContext dmc) {
        this(dmc, 1);
    }
    public MIExecReverseStepInstruction(IExecutionDMContext dmc, int count) {
        super(dmc, "-exec-stepi", new String[] { Integer.toString(count) }); //$NON-NLS-1$
    }
}