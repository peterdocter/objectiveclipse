/**********************************************************************
 * Copyright (c) 2004 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTDeclarator;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTParameterDeclaration;

/**
 * @author jcamelon
 */
public class CPPASTParameterDeclaration extends CPPASTNode implements
        ICPPASTParameterDeclaration {

    private IASTDeclSpecifier declSpec;
    private IASTDeclarator declarator;

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration#getDeclSpecifier()
     */
    public IASTDeclSpecifier getDeclSpecifier() {
        return declSpec;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration#getDeclarator()
     */
    public IASTDeclarator getDeclarator() {
        return declarator;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration#setDeclSpecifier(org.eclipse.cdt.core.dom.ast.IASTDeclSpecifier)
     */
    public void setDeclSpecifier(IASTDeclSpecifier declSpec) {
        this.declSpec = declSpec;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTParameterDeclaration#setDeclarator(org.eclipse.cdt.core.dom.ast.IASTDeclarator)
     */
    public void setDeclarator(IASTDeclarator declarator) {
        this.declarator = declarator;
    }

    public boolean accept( ASTVisitor action ){
        if( action.shouldVisitParameterDeclarations ){
		    switch( action.visit( this ) ){
	            case ASTVisitor.PROCESS_ABORT : return false;
	            case ASTVisitor.PROCESS_SKIP  : return true;
	            default : break;
	        }
		}
        
        if( declSpec != null ) if( !declSpec.accept( action ) ) return false;
        if( declarator != null ) if( !declarator.accept( action ) ) return false;    
        return true;
    }
}
