/*
 * AutoRefactor - Eclipse plugin to automatically refactor Java code bases.
 *
 * Copyright (C) 2015 Jean-Noël Rouvignac - initial API and implementation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program under LICENSE-GNUGPL.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution under LICENSE-ECLIPSE, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.autorefactor.refactoring.rules;

import static org.autorefactor.refactoring.ASTHelper.*;

import java.util.List;

import org.autorefactor.refactoring.ASTBuilder;
import org.autorefactor.refactoring.Refactorings;
import org.autorefactor.util.NotImplementedException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.InfixExpression.Operator;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.Statement;

/**
 * Refactors the use of TestNG assertions.
 * <p>
 * FIXME: Assert.assertNotEquals() exists only since TestNG 6.1.
 * This refactoring should be made conditional on TestNG version.
 * </p>
 */
public class TestNGAssertRefactoring extends AbstractRefactoringRule {

    private static final String OBJECT = "java.lang.Object";

    /** {@inheritDoc} */
    @Override
    public boolean visit(MethodInvocation node) {
        if (isMethod(node, "org.testng.Assert", "assertTrue", "boolean")
                || isMethod(node, "org.testng.Assert", "assertTrue", "boolean", "java.lang.String")) {
            return maybeRefactorAssertTrue(node, true);
        } else if (isMethod(node, "org.testng.Assert", "assertFalse", "boolean")
                || isMethod(node, "org.testng.Assert", "assertFalse", "boolean", "java.lang.String")) {
            return maybeRefactorAssertTrue(node, false);
        } else if (isMethod(node, "org.testng.Assert", "assertEquals", OBJECT, OBJECT)
                || isMethod(node, "org.testng.Assert", "assertEquals", OBJECT, OBJECT, "java.lang.String")) {
            return maybeRefactorAssertEquals(node, true);
        } else if (isMethod(node, "org.testng.Assert", "assertNotEquals", OBJECT, OBJECT)
                || isMethod(node, "org.testng.Assert", "assertNotEquals", OBJECT, OBJECT, "java.lang.String")) {
            return maybeRefactorAssertEquals(node, false);
        }
        return VISIT_SUBTREE;
    }

    private boolean maybeRefactorAssertTrue(MethodInvocation node, boolean isAssertTrue) {
        final List<Expression> args = arguments(node);
        final Expression arg0 = args.get(0);
        final InfixExpression arg0Ie = as(arg0, InfixExpression.class);
        final MethodInvocation arg0mi = as(arg0, MethodInvocation.class);
        final PrefixExpression arg0pe = as(arg0, PrefixExpression.class);
        final Refactorings r = this.ctx.getRefactorings();
        if (arg0Ie != null) {
            if (Operator.EQUALS.equals(arg0Ie.getOperator())) {
                return invokeAssert(node, arg0Ie, !isAssertTrue);
            } else if (Operator.NOT_EQUALS.equals(arg0Ie.getOperator())) {
                return invokeAssert(node, arg0Ie, isAssertTrue);
            }
        } else if (isMethod(arg0mi, OBJECT, "equals", OBJECT)) {
            r.replace(node,
                    invokeAssertEquals(node, arg0mi, !isAssertTrue));
            return DO_NOT_VISIT_SUBTREE;
        } else if (arg0pe != null && PrefixExpression.Operator.NOT.equals(arg0pe.getOperator())) {
            final MethodInvocation negatedMi = as(arg0pe.getOperand(), MethodInvocation.class);
            if (isMethod(negatedMi, OBJECT, "equals", OBJECT)) {
                r.replace(node,
                        invokeAssertEquals(node, negatedMi, isAssertTrue));
                return DO_NOT_VISIT_SUBTREE;
            }
        } else {
            Object constantValue = arg0.resolveConstantExpressionValue();
            if ((!isAssertTrue && Boolean.TRUE.equals(constantValue))
                    || (isAssertTrue && Boolean.FALSE.equals(constantValue))) {
                r.replace(node, invokeFail(node));
                return DO_NOT_VISIT_SUBTREE;
            }
        }
        return VISIT_SUBTREE;
    }

    private boolean maybeRefactorAssertEquals(MethodInvocation node, boolean isAssertEquals) {
        final Refactorings r = this.ctx.getRefactorings();
        final List<Expression> args = arguments(node);
        final Expression arg0 = args.get(0);
        final Expression arg1 = args.get(1);
        if (isNullLiteral(arg0)) {
            r.replace(node,
                    invokeAssertNull(node, !isAssertEquals, arg1, getMessageArg(node, 2)));
            return DO_NOT_VISIT_SUBTREE;
        } else if (isNullLiteral(arg1)) {
            r.replace(node,
                    invokeAssertNull(node, !isAssertEquals, arg0, getMessageArg(node, 2)));
            return DO_NOT_VISIT_SUBTREE;
        }
        return VISIT_SUBTREE;
    }

    private String getAssertName(boolean isNot, String assertType) {
        return "assert" + (isNot ? "Not" : "") + assertType;
    }

    private boolean invokeAssert(MethodInvocation node, final InfixExpression ie, boolean isNot) {
        final Refactorings r = this.ctx.getRefactorings();
        if (isComparingObjects(ie)) {
            if (isNullLiteral(ie.getLeftOperand())) {
                r.replace(node,
                        invokeAssertNull(node, isNot, ie.getRightOperand(), getMessageArg(node, 1)));
            } else if (isNullLiteral(ie.getRightOperand())) {
                r.replace(node,
                        invokeAssertNull(node, isNot, ie.getLeftOperand(), getMessageArg(node, 1)));
            } else {
                r.replace(node,
                        invokeAssert(node, getAssertName(isNot, "Same"), ie.getLeftOperand(), ie.getRightOperand()));
            }
        } else {
            r.replace(node,
                    invokeAssert(node, getAssertName(isNot, "Equals"), ie.getLeftOperand(), ie.getRightOperand()));
        }
        return DO_NOT_VISIT_SUBTREE;
    }

    private Expression getMessageArg(MethodInvocation node, int index) {
        final List<Expression> args = arguments(node);
        if (index < args.size()) {
            return args.get(index);
        }
        return null;
    }

    private ASTNode invokeAssertNull(MethodInvocation node, boolean isNot, Expression expr, Expression msgArg) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        final Expression copyOfExpr = node.getExpression() != null ? b.copy(node.getExpression()) : null;
        final String methodName = getAssertName(isNot, "Null");
        final Expression copyOfActual = b.copy(expr);
        if (msgArg == null) {
            return b.invoke(copyOfExpr, methodName, copyOfActual);
        } else {
            return b.invoke(copyOfExpr, methodName, copyOfActual, b.copy(msgArg));
        }
    }

    private boolean isComparingObjects(final InfixExpression ie) {
        return !isPrimitive(ie.getLeftOperand()) || !isPrimitive(ie.getRightOperand());
    }

    private MethodInvocation invokeAssert(MethodInvocation node, String methodName,
            Expression actual, Expression expected) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        return invokeAssertPriv(node, methodName, b.copy(actual), b.copy(expected), getMessageArg(node, 1));
    }

    private MethodInvocation invokeAssertEquals(MethodInvocation node, final MethodInvocation arg0mi, boolean isNot) {
        return invokeAssert(node, getAssertName(isNot, "Equals"), arg0mi.getExpression(), arguments(arg0mi));
    }

    private MethodInvocation invokeAssert(MethodInvocation node, String methodName,
            Expression actual, List<Expression> expected) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        return invokeAssertPriv(node, methodName, b.copy(actual), b.copyRange(expected), getMessageArg(node, 1));
    }

    private MethodInvocation invokeAssertPriv(MethodInvocation node, String methodName,
        Expression copyOfActual, Expression copyOfExpected, Expression msgArg) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        final Expression copyOfExpr = node.getExpression() != null ? b.copy(node.getExpression()) : null;
        if (msgArg == null) {
            return b.invoke(copyOfExpr, methodName, copyOfActual, copyOfExpected);
        } else {
            return b.invoke(copyOfExpr, methodName, copyOfActual, copyOfExpected, b.copy(msgArg));
        }
    }

    private MethodInvocation invokeFail(MethodInvocation node) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        final List<Expression> args = arguments(node);
        final Expression copyOfExpr = node.getExpression() != null ? b.copy(node.getExpression()) : null;
        if (args.size() == 1) {
            return b.invoke(copyOfExpr, "fail");
        } else if (args.size() == 2) {
            return b.invoke(copyOfExpr, "fail", b.copy(args.get(1)));
        } else {
            throw new NotImplementedException(node);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean visit(IfStatement node) {
        final List<Statement> stmts = asList(node.getThenStatement());
        if (stmts.size() == 1) {
            final MethodInvocation mi = asExpression(stmts.get(0), MethodInvocation.class);
            if (isMethod(mi, "org.testng.Assert", "fail")
                    || isMethod(mi, "org.testng.Assert", "fail", "java.lang.String")) {
                final InfixExpression conditionIe = as(node.getExpression(), InfixExpression.class);
                final MethodInvocation conditionMi = as(node.getExpression(), MethodInvocation.class);
                final PrefixExpression conditionPe = as(node.getExpression(), PrefixExpression.class);
                final Refactorings r = this.ctx.getRefactorings();
                if (conditionIe != null) {
                    if (Operator.EQUALS.equals(conditionIe.getOperator())) {
                        return invokeAssertForFail(node, mi, conditionIe, true);
                    } else if (Operator.NOT_EQUALS.equals(conditionIe.getOperator())) {
                        return invokeAssertForFail(node, mi, conditionIe, false);
                    }
                } else if (isMethod(conditionMi, OBJECT, "equals", OBJECT)) {
                    r.replace(node,
                            invokeAssertForFail(mi, "assertNotEquals",
                                    conditionMi.getExpression(), arguments(conditionMi).get(0)));
                    return DO_NOT_VISIT_SUBTREE;
                } else if (conditionPe != null && PrefixExpression.Operator.NOT.equals(conditionPe.getOperator())) {
                    final MethodInvocation negatedMi = as(conditionPe.getOperand(), MethodInvocation.class);
                    if (isMethod(negatedMi, OBJECT, "equals", OBJECT)) {
                        r.replace(node,
                                invokeAssertForFail(mi, "assertEquals",
                                        negatedMi.getExpression(), arguments(negatedMi).get(0)));
                        return DO_NOT_VISIT_SUBTREE;
                    }
                }
            }
        }
        return VISIT_SUBTREE;
    }

    private boolean invokeAssertForFail(IfStatement toReplace, MethodInvocation mi, InfixExpression ie, boolean isNot) {
        final Refactorings r = this.ctx.getRefactorings();
        if (isComparingObjects(ie)) {
            r.replace(toReplace,
                    invokeAssertForFail(mi, getAssertName(isNot, "Same"), ie.getLeftOperand(), ie.getRightOperand()));
        } else {
            r.replace(toReplace,
                    invokeAssertForFail(mi, getAssertName(isNot, "Equals"), ie.getLeftOperand(), ie.getRightOperand()));
        }
        return DO_NOT_VISIT_SUBTREE;
    }

    private ExpressionStatement invokeAssertForFail(MethodInvocation mi, String methodName,
            Expression actual, Expression expected) {
        final ASTBuilder b = this.ctx.getASTBuilder();
        return b.toStmt(
                invokeAssertPriv(mi, methodName, b.copy(actual), b.copy(expected), getMessageArg(mi, 0)));
    }
}
