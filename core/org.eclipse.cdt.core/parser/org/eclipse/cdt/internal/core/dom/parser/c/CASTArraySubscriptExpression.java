/**********************************************************************
 * Copyright (c) 2002-2004 IBM Canada and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM Rational Software - Initial API and implementation */
package org.eclipse.cdt.internal.core.dom.parser.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression;
import org.eclipse.cdt.core.dom.ast.IASTExpression;

/**
 * @author jcamelon
 */
public class CASTArraySubscriptExpression extends CASTNode implements
        IASTArraySubscriptExpression {

    private IASTExpression array;
    private IASTExpression subscript;

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression#getArrayExpression()
     */
    public IASTExpression getArrayExpression() {
        return array;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression#setArrayExpression(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    public void setArrayExpression(IASTExpression expression) {
        array = expression;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression#getSubscriptExpression()
     */
    public IASTExpression getSubscriptExpression() {
        return subscript;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.dom.ast.IASTArraySubscriptExpression#setSubscriptExpression(org.eclipse.cdt.core.dom.ast.IASTExpression)
     */
    public void setSubscriptExpression(IASTExpression expression) {
        this.subscript = expression;
    }

    public boolean accept( ASTVisitor action ){
        if( action.shouldVisitExpressions ){
		    switch( action.visit( this ) ){
	            case ASTVisitor.PROCESS_ABORT : return false;
	            case ASTVisitor.PROCESS_SKIP  : return true;
	            default : break;
	        }
		}
        
        if( array != null ) if( !array.accept( action ) ) return false;
        if( subscript != null ) if( !subscript.accept( action ) ) return false;
        return true;
    }
}
