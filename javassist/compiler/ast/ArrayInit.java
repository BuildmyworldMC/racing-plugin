package javassist.compiler.ast;

import javassist.compiler.CompileError;

public class ArrayInit extends ASTList {
   private static final long serialVersionUID = 1L;

   public ArrayInit(ASTree firstElement) {
      super(firstElement);
   }

   public void accept(Visitor v) throws CompileError {
      v.atArrayInit(this);
   }

   public String getTag() {
      return "array";
   }
}
