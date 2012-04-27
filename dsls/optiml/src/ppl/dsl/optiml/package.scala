package ppl.dsl

package object optiml {
  /////////////////////////
  // inherited from OptiLA
  //
  // is there a less manual way of doing this?

  type Vector[T] = optila.Vector[T]
  type DenseVector[T] = optila.DenseVector[T]
  type SparseVector[T] = optila.SparseVector[T]
  type RowVector[T] = optila.RowVector[T]
  type ColVector[T] = optila.ColVector[T]
  //type ZeroVector[T] = optila.ZeroVector[T]
  //type EmptyVector[T] = optila.EmptyVector[T]
  type RangeVector = optila.RangeVector
  type VectorView[T] = optila.VectorView[T]
  //type MatrixRow[T] = optila.MatrixRow[T]
  //type MatrixCol[T] = optila.MatrixCol[T]

  type Matrix[T] = optila.Matrix[T]
  type DenseMatrix[T] = optila.DenseMatrix[T]
  type SparseMatrix[T] = optila.SparseMatrix[T]
  //type SymmetricMatrix[T] = optila.SymmetricMatrix[T]
}
