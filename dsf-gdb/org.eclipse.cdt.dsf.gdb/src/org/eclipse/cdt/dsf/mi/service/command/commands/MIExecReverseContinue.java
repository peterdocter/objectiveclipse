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
 * Resume backwards. 
 * 
 * @since 2.0
 */
public class MIExecReverseContinue extends MICommand<MIInfo> {

    public MIExecReverseContinue(IExecutionDMContext dmc) {
        super(dmc, "-exec-continue"); //$NON-NLS-1$
    }
}