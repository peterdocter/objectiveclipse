/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.dom.lrparser.gpp;

import org.eclipse.cdt.core.dom.ILinkage;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.lrparser.BaseExtensibleLanguage;
import org.eclipse.cdt.core.dom.lrparser.IDOMTokenMap;
import org.eclipse.cdt.core.dom.lrparser.IParser;
import org.eclipse.cdt.core.dom.parser.IScannerExtensionConfiguration;
import org.eclipse.cdt.core.dom.parser.c.GCCScannerExtensionConfiguration;
import org.eclipse.cdt.core.model.IContributedModelBuilder;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.core.parser.ParserLanguage;
import org.eclipse.cdt.internal.core.dom.lrparser.gpp.GPPParser;
import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPNodeFactory;

/**
 * ILanguage implementation for the GPP parser.
 * 
 * @author Mike Kucera
 */
@SuppressWarnings("restriction")
public class GPPLanguage extends BaseExtensibleLanguage {

	public static final String ID = "org.eclipse.cdt.core.lrparser.gpp"; //$NON-NLS-1$ 
	
	private static GPPLanguage DEFAULT = new GPPLanguage();
	
	public static GPPLanguage getDefault() {
		return DEFAULT;
	}
	
	@Override
	protected IParser getParser() {
		return new GPPParser();
	}

	@Override
	protected IDOMTokenMap getTokenMap() {
		return DOMToGPPTokenMap.DEFAULT_MAP;
	}

	@Override
	protected IScannerExtensionConfiguration getScannerExtensionConfiguration() {
		return GCCScannerExtensionConfiguration.getInstance();
	}
	
	public IContributedModelBuilder createModelBuilder(@SuppressWarnings("unused") ITranslationUnit tu) {
		return null;
	}

	public String getId() {
		return ID;
	}

	public int getLinkageID() {
		return ILinkage.CPP_LINKAGE_ID;
	}

	@Override
	protected ParserLanguage getParserLanguage() {
		return ParserLanguage.CPP;
	}

	@Override
	protected IASTTranslationUnit createASTTranslationUnit() {
		return CPPNodeFactory.getDefault().newTranslationUnit();
	}

}