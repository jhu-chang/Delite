package ppl.dsl.optila.vector

import java.io.{PrintWriter}
import scala.reflect.{Manifest,SourceContext}
import scala.collection.mutable.HashMap

import scala.virtualization.lms.common._
import scala.virtualization.lms.internal.{GenerationFailedException, GenericFatCodegen}

import ppl.delite.framework.DeliteApplication
import ppl.delite.framework.ops.{DeliteOpsExp, DeliteCollectionOpsExp}
import ppl.delite.framework.datastruct.scala.DeliteCollection
import ppl.dsl.optila._

trait SparseVectorOps extends Variables {
  this: OptiLA =>

  implicit def repToSparseVecOps[A:Manifest](x: Rep[SparseVector[A]]) = new SparseVecOpsCls(x)
  implicit def varToSparseVecOps[A:Manifest](x: Var[SparseVector[A]]) = new SparseVecOpsCls(readVar(x))
  implicit def sparseVecToInterface[A:Manifest](lhs: Rep[SparseVector[A]]) = new VInterface[A](new SparseVecOpsCls[A](lhs))

  implicit def sparseVectorBuilder[A:Manifest](implicit ctx: SourceContext) = new VectorBuilder[A,SparseVector[A]] {
    def alloc(length: Rep[Int], isRow: Rep[Boolean]) = {
      sparsevector_obj_new[A](length, isRow)
    }
    def toIntf(x: Rep[SparseVector[A]]): Interface[Vector[A]] = sparseVecToInterface(x)
  }  

  object SparseVector {
    def apply[A:Manifest](len: Int, isRow: Boolean)(implicit ctx: SourceContext) = sparsevector_obj_new(unit(len), unit(isRow)) // needed to resolve ambiguities
    def apply[A:Manifest](len: Rep[Int], isRow: Rep[Boolean])(implicit o: Overloaded1, ctx: SourceContext) = sparsevector_obj_new(len, isRow)
    def flatten[A:Manifest](pieces: Rep[SparseVector[SparseVector[A]]])(implicit ctx: SourceContext) = sparsevector_obj_flatten(pieces)
  }
  
  class SparseVecOpsCls[A:Manifest](val elem: Rep[SparseVector[A]]) extends VecOpsCls[A] {
    type V[X] = SparseVector[X]        
    type M[X] = SparseMatrix[X]        
    type Self = SparseVector[A]
    
    def wrap(x: Rep[SparseVector[A]]) = sparseVecToInterface(x)
    def toOps[B:Manifest](x: Rep[SparseVector[B]]) = repToSparseVecOps(x)
    def toIntf[B:Manifest](x: Rep[SparseVector[B]]): Interface[Vector[B]] = sparseVecToInterface(x)
    def matToIntf[B:Manifest](x: Rep[SparseMatrix[B]]): Interface[Matrix[B]] = throw new UnsupportedOperationException("tbd") //sparseMatToInterface(x)  
    def builder[B:Manifest](implicit ctx: SourceContext): VectorBuilder[B,V[B]] = sparseVectorBuilder[B]    
    def matBuilder[B:Manifest](implicit ctx: SourceContext): MatrixBuilder[B,M[B]] = throw new UnsupportedOperationException("tbd") //sparseMatrixBuilder[B] 
    def mV[B:Manifest] = manifest[SparseVector[B]] 
    def mM[B:Manifest] = manifest[SparseMatrix[B]] 
    def mA: Manifest[A] = manifest[A]        
    
    // accessors
    def length(implicit ctx: SourceContext) = sparsevector_length(elem)
    def isRow(implicit ctx: SourceContext) = sparsevector_isrow(elem)
    def apply(n: Rep[Int])(implicit ctx: SourceContext) = sparsevector_apply(elem, n)
    
    // general
    def t(implicit ctx: SourceContext) = sparsevector_trans(elem)
    def mt()(implicit ctx: SourceContext) = {sparsevector_mutable_trans(elem); elem}
    
    // data operations
    def update(n: Rep[Int], y: Rep[A])(implicit ctx: SourceContext) = sparsevector_update(elem,n,y)
    def copyFrom(pos: Rep[Int], y: Rep[SparseVector[A]])(implicit ctx: SourceContext) = sparsevector_copyfrom(elem,pos,y)
    def insert(pos: Rep[Int], y: Rep[A])(implicit ctx: SourceContext) = sparsevector_insert(elem,pos,y)
    def insertAll(pos: Rep[Int], y: Rep[SparseVector[A]])(implicit ctx: SourceContext) = sparsevector_insertall(elem,pos,y)
    def removeAll(pos: Rep[Int], len: Rep[Int])(implicit ctx: SourceContext) = sparsevector_removeall(elem,pos,len)
    def trim()(implicit ctx: SourceContext) = sparsevector_trim(elem)
    def clear()(implicit ctx: SourceContext) = sparsevector_clear(elem)
        
    // generic arithmetic
    type VPLUSR = DenseVector[A]
    val mVPLUSR = manifest[VPLUSR]
    def vplusBuilder(implicit ctx: SourceContext) = denseVectorBuilder[A]
    def vplusToIntf(x: Rep[VPLUSR]) = denseVecToInterface(x)

    type VMINUSR = DenseVector[A]
    val mVMINUSR = manifest[VMINUSR]
    def vminusBuilder(implicit ctx: SourceContext) = denseVectorBuilder[A]
    def vminusToIntf(x: Rep[VMINUSR]) = denseVecToInterface(x)    
    
    type VTIMESR = SparseVector[A]
    val mVTIMESR = manifest[VTIMESR]
    def vtimesBuilder(implicit ctx: SourceContext) = builder[A]
    def vtimesToIntf(x: Rep[VTIMESR]) = toIntf(x)        
    
    def *(y: Rep[SparseMatrix[A]])(implicit a: Arith[A], o: Overloaded2, ctx: SourceContext) = sparsevector_times_matrix(elem,y)
    
    // ordering operations
    def sort(implicit o: Ordering[A], ctx: SourceContext) = sparsevector_sort(elem)    
  }

  // object defs
  def sparsevector_obj_new[A:Manifest](len: Rep[Int], isRow: Rep[Boolean])(implicit ctx: SourceContext): Rep[SparseVector[A]]
  def sparsevector_obj_flatten[A:Manifest](pieces: Rep[SparseVector[SparseVector[A]]])(implicit ctx: SourceContext): Rep[SparseVector[A]]  
  
  // class defs
  def sparsevector_length[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Int]
  def sparsevector_isrow[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Boolean]
  def sparsevector_apply[A:Manifest](x: Rep[SparseVector[A]], n: Rep[Int])(implicit ctx: SourceContext): Rep[A]  
  def sparsevector_trans[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[SparseVector[A]]
  def sparsevector_mutable_trans[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_update[A:Manifest](x: Rep[SparseVector[A]], n: Rep[Int], y: Rep[A])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_copyfrom[A:Manifest](x: Rep[SparseVector[A]], pos: Rep[Int], y: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_insert[A:Manifest](x: Rep[SparseVector[A]], pos: Rep[Int], y: Rep[A])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_insertall[A:Manifest](x: Rep[SparseVector[A]], pos: Rep[Int], y: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_removeall[A:Manifest](x: Rep[SparseVector[A]], pos: Rep[Int], len: Rep[Int])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_trim[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_clear[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_times_matrix[A:Manifest:Arith](x: Rep[SparseVector[A]], y: Rep[SparseMatrix[A]])(implicit ctx: SourceContext): Rep[SparseVector[A]]
  def sparsevector_sort[A:Manifest:Ordering](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[SparseVector[A]]
}

trait SparseVectorCompilerOps extends SparseVectorOps {
  this: OptiLACompiler =>
  
  def sparsevector_raw_data[A:Manifest](x: Rep[SparseVector[A]])(implicit ctx: SourceContext): Rep[HashMap[Int,A]]
  def sparsevector_set_raw_data[A:Manifest](x: Rep[SparseVector[A]], data: Rep[HashMap[Int,A]])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_set_length[A:Manifest](x: Rep[SparseVector[A]], newVal: Rep[Int])(implicit ctx: SourceContext): Rep[Unit]
  def sparsevector_set_isrow[A:Manifest](x: Rep[SparseVector[A]], newVal: Rep[Boolean])(implicit ctx: SourceContext): Rep[Unit]
  
  /* 
   In order to define a SparseVector of a new type T, you must override the
   defaultValue method for that type (inside *ApplicationRunner currently)
  */
  def defaultValue[A:Manifest] = {
    if (manifest[A] <:< manifest[AnyVal]) unit(0.asInstanceOf[A]) // a bit of a 'loose' approximation
    else err("no default value found for type: " + manifest[A])
  }  
}

trait SparseVectorOpsExp extends SparseVectorOps with VariablesExp with BaseFatExp {

  this: SparseVectorImplOps with OptiLAExp =>
      
  ///////////////////////////////////////////////////
  // implemented via method on real data structure
  
  case class SparseVectorNew[A:Manifest](len: Exp[Int], isRow: Exp[Boolean]) extends DefWithManifest[A,SparseVector[A]] 
  case class SparseVectorLength[A:Manifest](x: Exp[SparseVector[A]]) extends DefWithManifest[A,Int]
  case class SparseVectorIsRow[A:Manifest](x: Exp[SparseVector[A]]) extends DefWithManifest[A,Boolean]
  case class SparseVectorRawData[A:Manifest](x: Exp[SparseVector[A]]) extends DefWithManifest[A,HashMap[Int,A]]
  case class SparseVectorSetLength[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[Int]) extends DefWithManifest[A,Unit]
  case class SparseVectorSetIsRow[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[Boolean]) extends DefWithManifest[A,Unit]
  case class SparseVectorSetRawData[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[HashMap[Int,A]]) extends DefWithManifest[A,Unit]
    
  /////////////////////////////////////////////////
  // implemented via kernel embedding (sequential)
  
  case class SparseVectorObjectFlatten[A:Manifest](pieces: Exp[SparseVector[SparseVector[A]]])
    extends DeliteOpSingleWithManifest[A,SparseVector[A]](reifyEffectsHere(sparsevector_obj_flatten_impl(pieces)))
        
  case class SparseVectorApply[A:Manifest](x: Exp[SparseVector[A]], n: Exp[Int]) 
      extends DefWithManifest[A,A]
  //  extends DeliteOpSingleWithManifest[A,A](reifyEffectsHere(sparsevector_apply_impl(x,n)))
    
  case class SparseVectorUpdate[A:Manifest](x: Exp[SparseVector[A]], n: Exp[Int], y: Exp[A]) 
      extends DefWithManifest[A,Unit]
  //  extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_update_impl(x,n,y)))
    
  case class SparseVectorCopyFrom[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[SparseVector[A]])
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_copyfrom_impl(x,pos,y)))
    
  case class SparseVectorInsert[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[A]) 
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_insert_impl(x,pos,y)))
    
  case class SparseVectorInsertAll[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[SparseVector[A]])
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_insertall_impl(x,pos,y)))
    
  case class SparseVectorRemoveAll[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], len: Exp[Int])
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_removeall_impl(x,pos,len)))
    
  case class SparseVectorTrim[A:Manifest](x: Exp[SparseVector[A]]) 
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_trim_impl(x)))
    
  case class SparseVectorClear[A:Manifest](x: Exp[SparseVector[A]]) 
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_clear_impl(x)))
    
  case class SparseVectorMutableTrans[A:Manifest](x: Exp[SparseVector[A]]) 
    extends DeliteOpSingleWithManifest[A,Unit](reifyEffectsHere(sparsevector_mutabletrans_impl(x)))
    
  case class SparseVectorSort[A:Manifest:Ordering](x: Exp[SparseVector[A]]) 
    extends DeliteOpSingleWithManifest[A,SparseVector[A]](reifyEffectsHere(sparsevector_sort_impl(x))) {
   
    val o = implicitly[Ordering[A]]   
  }

  case class SparseVectorTimesMatrix[A:Manifest:Arith](x: Exp[SparseVector[A]], y: Exp[SparseMatrix[A]])
    extends DeliteOpSingleWithManifest[A,SparseVector[A]](reifyEffectsHere(sparsevector_times_matrix_impl[A](x,y))) {
    
    val a = implicitly[Arith[A]]
  }  
  
  ////////////////////////////////
  // implemented via delite ops

  case class SparseVectorTrans[A:Manifest](in: Exp[SparseVector[A]])
    extends DeliteOpMap[A,A,SparseVector[A]] {
    val size = copyTransformedOrElse(_.size)(in.length)

    def alloc = SparseVector[A](in.length, !in.isRow)
    def func = e => e 

    val mA = manifest[A]
  }

  /////////////////////
  // object interface

  def sparsevector_obj_new[A:Manifest](len: Exp[Int], isRow: Exp[Boolean])(implicit ctx: SourceContext) = reflectMutable(SparseVectorNew[A](len, isRow)) //XXX
  def sparsevector_obj_flatten[A:Manifest](pieces: Exp[SparseVector[SparseVector[A]]])(implicit ctx: SourceContext) = reflectPure(SparseVectorObjectFlatten(pieces))  

  /////////////////////
  // class interface

  def sparsevector_length[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorLength(x))
  def sparsevector_isrow[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorIsRow(x))
  def sparsevector_apply[A:Manifest](x: Exp[SparseVector[A]], n: Exp[Int])(implicit ctx: SourceContext) = reflectPure(SparseVectorApply(x, n))
  def sparsevector_trans[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorTrans(x))
  def sparsevector_mutable_trans[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorMutableTrans(x))
  def sparsevector_update[A:Manifest](x: Exp[SparseVector[A]], n: Exp[Int], y: Exp[A])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorUpdate(x, n, y))
  def sparsevector_copyfrom[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorCopyFrom(x, pos, y))
  def sparsevector_insert[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[A])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorInsert(x, pos, y))
  def sparsevector_insertall[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], y: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorInsertAll(x, pos, y))
  def sparsevector_removeall[A:Manifest](x: Exp[SparseVector[A]], pos: Exp[Int], len: Exp[Int])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorRemoveAll(x, pos, len))
  def sparsevector_trim[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorTrim(x))
  def sparsevector_clear[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorClear(x))
  def sparsevector_times_matrix[A:Manifest:Arith](x: Exp[SparseVector[A]], y: Exp[SparseMatrix[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorTimesMatrix(x,y))
  def sparsevector_sort[A:Manifest:Ordering](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorSort(x))
  
  /////////////
  // internal
  
  def sparsevector_raw_data[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = reflectPure(SparseVectorRawData(x))
  def sparsevector_set_raw_data[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[HashMap[Int,A]])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorSetRawData(x, newVal))
  def sparsevector_set_length[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[Int])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorSetLength(x, newVal))
  def sparsevector_set_isrow[A:Manifest](x: Exp[SparseVector[A]], newVal: Exp[Boolean])(implicit ctx: SourceContext) = reflectWrite(x)(SparseVectorSetIsRow(x, newVal))
    
  
  //////////////
  // mirroring

  override def mirror[A:Manifest](e: Def[A], f: Transformer)(implicit ctx: SourceContext): Exp[A] = (e match {
    case e@SparseVectorLength(x) => reflectPure(SparseVectorLength(f(x))(e.mA))(mtype(manifest[A]),implicitly[SourceContext])
    case e@SparseVectorIsRow(x) => reflectPure(SparseVectorIsRow(f(x))(e.mA))(mtype(manifest[A]),implicitly[SourceContext])
    case e@SparseVectorRawData(x) => reflectPure(SparseVectorRawData(f(x))(e.mA))(mtype(manifest[A]),implicitly[SourceContext])
    case e@SparseVectorApply(x,n) => reflectPure(SparseVectorApply(f(x),f(n))(e.mA))(mtype(manifest[A]),implicitly[SourceContext])
    
    // delite ops
    case e@SparseVectorSort(x) => reflectPure(new { override val original = Some(f,e) } with SparseVectorSort(f(x))(e.mA,e.o))(mtype(manifest[A]),implicitly[SourceContext])
    case e@SparseVectorTimesMatrix(x,y) => reflectPure(new { override val original = Some(f,e) } with SparseVectorTimesMatrix(f(x),f(y))(e.mA,e.a))(mtype(manifest[A]),implicitly[SourceContext])
    case e@SparseVectorTrans(x) => reflectPure(new { override val original = Some(f,e) } with SparseVectorTrans(f(x))(e.mA))(mtype(manifest[A]),implicitly[SourceContext])
    // read/write effects
    case Reflect(e@SparseVectorNew(l,r), u, es) => reflectMirrored(Reflect(SparseVectorNew(f(l),f(r))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))    
    case Reflect(e@SparseVectorSetLength(x,v), u, es) => reflectMirrored(Reflect(SparseVectorSetLength(f(x),f(v))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))    
    case Reflect(e@SparseVectorSetIsRow(x,v), u, es) => reflectMirrored(Reflect(SparseVectorSetIsRow(f(x),f(v))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))  
    case Reflect(e@SparseVectorSetRawData(x,v), u, es) => reflectMirrored(Reflect(SparseVectorSetRawData(f(x),f(v))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))              
    case Reflect(e@SparseVectorLength(x), u, es) => reflectMirrored(Reflect(SparseVectorLength(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorIsRow(x), u, es) => reflectMirrored(Reflect(SparseVectorIsRow(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))    
    case Reflect(e@SparseVectorRawData(x), u, es) => reflectMirrored(Reflect(SparseVectorRawData(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorApply(x,n), u, es) => reflectMirrored(Reflect(SparseVectorApply(f(x),f(n))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorUpdate(x,n,y), u, es) => reflectMirrored(Reflect(SparseVectorUpdate(f(x),f(n),f(y))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorSort(x), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorSort(f(x))(e.mA,e.o), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorTimesMatrix(x,y), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorTimesMatrix(f(x),f(y))(e.mA,e.a), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorTrans(x), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorTrans(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorCopyFrom(x,pos,y), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorCopyFrom(f(x),f(pos),f(y))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorInsert(x,pos,y), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorInsert(f(x),f(pos),f(y))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorInsertAll(x,pos,y), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorInsertAll(f(x),f(pos),f(y))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorRemoveAll(x,pos,y), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorRemoveAll(f(x),f(pos),f(y))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorTrim(x), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorTrim(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorClear(x), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorClear(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))
    case Reflect(e@SparseVectorMutableTrans(x), u, es) => reflectMirrored(Reflect(new { override val original = Some(f,e) } with SparseVectorMutableTrans(f(x))(e.mA), mapOver(f,u), f(es)))(mtype(manifest[A]))    
    case _ => super.mirror(e, f)
  }).asInstanceOf[Exp[A]] // why??


  /////////////////////
  // aliases and sharing

  // TODO: precise sharing info for other IR types (default is conservative)
  
  override def aliasSyms(e: Any): List[Sym[Any]] = e match {
    case SparseVectorApply(a,i) => Nil
    case SparseVectorUpdate(a,i,x) => Nil           // syms(a) <-- any use to return a?
    case SparseVectorInsert(a,i,x) => Nil           // syms(a) <-- any use to return a?
    case SparseVectorInsertAll(a,i,x) => Nil        // syms(a) <-- any use to return a?
    case _ => super.aliasSyms(e)
  }

  override def containSyms(e: Any): List[Sym[Any]] = e match {
    case SparseVectorApply(a,i) => Nil
    case SparseVectorUpdate(a,i,x) => syms(x)
    case SparseVectorInsert(a,i,x) => syms(x)
    case SparseVectorInsertAll(a,i,x) => Nil
    case _ => super.containSyms(e)
  }

  override def extractSyms(e: Any): List[Sym[Any]] = e match {
    case SparseVectorApply(a,i) => syms(a)
    case SparseVectorUpdate(a,i,x) => Nil
    case SparseVectorInsert(a,i,x) => Nil
    case SparseVectorInsertAll(a,i,x) => Nil
    case _ => super.extractSyms(e)
  }

  override def copySyms(e: Any): List[Sym[Any]] = e match {
    case SparseVectorApply(a,i) => Nil
    case SparseVectorUpdate(a,i,x) => syms(a)
    case SparseVectorInsert(a,i,x) => syms(a)
    case SparseVectorInsertAll(a,i,x) => syms(a) ++ syms(x)
    case _ => super.copySyms(e)
  }  
}

/**
 * Optimizations for composite SparseVectorOps operations.
 */

// have to extend DeliteCollectionOps to override dc_apply...
trait SparseVectorOpsExpOpt extends SparseVectorOpsExp with DeliteCollectionOpsExp {
  this: SparseVectorImplOps with OptiLAExp =>

  override def sparsevector_length[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = x match {
    /* these are essential for fusing:    */
    case Def(SparseVectorNew(len, isRow)) => len

    /* propagate output size information */
    // some of this could be handled in DeliteCollectionOps, but we need a way to link length (for single tasks)
    // and size (for data parallel tasks) together. SparseVector can override dc_size, but has to deal with erasure.    
    case Def(e: DeliteOpMap[_,_,_]) => e.size
    case Def(e: DeliteOpZipWith[_,_,_,_]) => e.size
    case Def(SparseVectorSort(a)) => a.length
      
    case _ => 
      //printerr("could not short-circuit call to " + x.toString + ".length")
      //printerr(findDefinition(x.asInstanceOf[Sym[SparseVector[A]]]))
      super.sparsevector_length(x)
  }

  override def sparsevector_isrow[A:Manifest](x: Exp[SparseVector[A]])(implicit ctx: SourceContext) = x match {
    case Def(e: VectorArithmeticMap[A,_]) => e.intf.isRow 
    case Def(e: VectorArithmeticZipWith[A,_]) => e.intfA.isRow 
    case Def(VectorClone(a)) => a.isRow
    case _ => super.sparsevector_isrow(x)
  }
  
  def sparsevector_optimize_apply[A:Manifest](x: Exp[DeliteCollection[A]], n: Exp[Int])(implicit ctx: SourceContext): Option[Exp[A]] = x match {
    case Def(SparseVectorTrans(x)) => Some(sparsevector_apply(x,n))
    case _ => None
  }
  
  override def sparsevector_apply[A:Manifest](x: Exp[SparseVector[A]], n: Exp[Int])(implicit ctx: SourceContext) = {
    sparsevector_optimize_apply(x.asInstanceOf[Exp[DeliteCollection[A]]],n) getOrElse super.sparsevector_apply(x,n)
  }
  
  override def dc_apply[A:Manifest](x: Exp[DeliteCollection[A]], n: Exp[Int])(implicit ctx: SourceContext) = {
    sparsevector_optimize_apply(x,n) getOrElse super.dc_apply(x,n)
  }
}


trait BaseGenSparseVectorOps extends GenericFatCodegen {
  val IR: SparseVectorOpsExp
  import IR._
  
  override def unapplySimpleIndex(e: Def[Any]) = e match { // TODO: move elsewhere
    case SparseVectorApply(a, i) => Some((a,i))
    case _ => super.unapplySimpleIndex(e)
  }  
}

trait ScalaGenSparseVectorOps extends BaseGenSparseVectorOps with ScalaGenFat {
  val IR: SparseVectorOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any])(implicit stream: PrintWriter) = rhs match {
    // these are the ops that call through to the underlying real data structure
    case v@SparseVectorNew(length, isRow) => emitValDef(sym, "new " + remap("generated.scala.SparseVector[" + remap(v.mA) + "]")+"(" + quote(length) + "," + quote(isRow) + ")")        
    case SparseVectorApply(x,n) => emitValDef(sym, quote(x) + "._data(" + quote(n) + ")")
    case SparseVectorUpdate(x,n,y) => emitValDef(sym, quote(x) + "._data(" + quote(n) + ") = " + quote(y))
    case SparseVectorLength(x) => emitValDef(sym, quote(x) + "._length")
    case SparseVectorIsRow(x) => emitValDef(sym, quote(x) + "._isRow")
    case SparseVectorRawData(x) => emitValDef(sym, quote(x) + "._data")
    case SparseVectorSetLength(x,v) => emitValDef(sym, quote(x) + "._length = " + quote(v))
    case SparseVectorSetIsRow(x,v) => emitValDef(sym, quote(x) + "._isRow = " + quote(v))
    case SparseVectorSetRawData(x,v) => emitValDef(sym, quote(x) + "._data = " + quote(v))
    case _ => super.emitNode(sym, rhs)
  }
}

trait CudaGenSparseVectorOps extends BaseGenSparseVectorOps with CudaGenFat with CudaGenDataStruct {
  val IR: SparseVectorOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any])(implicit stream: PrintWriter) = rhs match {
    case _ => super.emitNode(sym, rhs)
  }
}

trait OpenCLGenSparseVectorOps extends BaseGenSparseVectorOps with OpenCLGenFat with OpenCLGenDataStruct {
  val IR: SparseVectorOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any])(implicit stream: PrintWriter) = rhs match {	
    case _ => super.emitNode(sym, rhs)
  }
}

trait CGenSparseVectorOps extends BaseGenSparseVectorOps with CGenFat {
  val IR: SparseVectorOpsExp
  import IR._

  override def emitNode(sym: Sym[Any], rhs: Def[Any])(implicit stream: PrintWriter) = rhs match {
    case _ => super.emitNode(sym, rhs)
  }
}

