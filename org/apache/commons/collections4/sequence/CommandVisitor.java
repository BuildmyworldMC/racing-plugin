package org.apache.commons.collections4.sequence;

public interface CommandVisitor<T> {
   void visitInsertCommand(T var1);

   void visitKeepCommand(T var1);

   void visitDeleteCommand(T var1);
}
