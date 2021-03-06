package net.teralytics.terahex

import scala.math._

/**
  * Hexagonal grid with flat-top hexagons and axial coordinates based on the Amit Patel's guide:
  * http://www.redblobgames.com/grids/hexagons/
  *
  */

/**
  * Coordinates in a 2D orthogonal coordinate system X,Y, where X axis is pointing rightwards and Y downwards.
  */
case class Point(x: Double, y: Double) {

  def rotate(aRadians: Double): Point = Point(
    x * cos(aRadians) - y * sin(aRadians),
    x * sin(aRadians) + y * cos(aRadians))

  def +(other: Point): Point = Point(x = x + other.x, y = y + other.y)

  def -(other: Point): Point = Point(x = x - other.x, y = y - other.y)

  def toHex: Hex = Hex(
    col = 2 * x / 3,
    row = (-x + sqrt(3) * y) / 3)
}

/**
  * Cube coordinates projected on a 2D plane. Only used by Hex coordinates for easier rounding.
  */
case class Cube(x: Double, y: Double, z: Double) {

  def +(other: Cube): Cube = Cube(x + other.x, y + other.y, z + other.z)

  def -(other: Cube): Cube = Cube(x - other.x, y - other.y, z - other.z)

  def scale(k: Double): Cube = Cube(k * x, k * y, k * z)

  def length: Double = max(max(abs(x), abs(y)), abs(z))

  def distanceTo(other: Cube): Double = (other - this).length

  def pathTo(other: Cube, steps: Int): Seq[Cube] =
    if (steps < 1) Seq(this)
    else {
      val step = (other - this).scale(1d / steps)
      Iterator.iterate(this)(_ + step).take(steps + 1).toSeq
    }

  def round = {
    var (rx, ry, rz) = (x.round, y.round, z.round)

    val (dx, dy, dz) = (abs(rx - x), abs(ry - y), abs(rz - z))

    if (dx > dy && dx > dz)
      rx = -ry - rz
    else if (dy > dz)
      ry = -rx - rz
    else
      rz = -rx - ry

    Cube(rx, ry, rz)
  }

  def toHex: Hex = Hex(col = x, row = z)
}

/**
  * Fractional coordinates in the hexagonal axial system with columns and rows.
  */
case class Hex(col: Double = 0, row: Double = 0) {

  def scale(k: Double): Hex = Hex(col = k * col, row = k * row)

  def +(other: Hex): Hex = Hex(col = col + other.col, row = row + other.row)

  def -(other: Hex): Hex = Hex(col = col - other.col, row = row - other.row)

  def toCube: Cube = Cube(x = col, y = -col - row, z = row)

  def round: Cell = {
    val Hex(col, row) = toCube.round.toHex
    Cell(col = col.toLong, row.toLong)
  }

  def distanceTo(other: Hex): Double = toCube.distanceTo(other.toCube)

  def pathTo(other: Hex, steps: Int): Seq[Hex] = toCube.pathTo(other.toCube, steps).map(_.toHex)

  def toPoint: Point = Point(
    x = 1.5 * col,
    y = sqrt(3) * (row + col / 2))

  def toCell(size: Double = 1): Cell = scale(1d / size).round
}

/**
  * Discrete cell coordinate in a hexagonal grid.
  */
case class Cell(col: Long = 0, row: Long = 0) {

  def toHex(size: Double = 1): Hex = Hex(col = col, row = row).scale(size)

  def moveN: Cell = Cell(col, row - 1)

  def moveS: Cell = Cell(col, row + 1)

  def moveNE: Cell = Cell(col + 1, row - 1)

  def moveSE: Cell = Cell(col + 1, row)

  def moveNW: Cell = Cell(col - 1, row)

  def moveSW: Cell = Cell(col - 1, row + 1)

  def distanceTo(other: Cell): Long = toHex().distanceTo(other.toHex()).round

  def pathTo(other: Cell): Seq[Cell] = {
    val steps = distanceTo(other).toInt
    val size = 1
    toHex(size).pathTo(other.toHex(size), steps).map(_.toCell(size))
  }
}

object Cell {

  val subCenter = Cell()
  val subN = subCenter.moveN
  val subS = subCenter.moveS
  val subE = subCenter.moveNE.moveSE
  val subW = subCenter.moveNW.moveSW
  val subNE = subCenter.moveNE
  val subNW = subCenter.moveNW
  val subSE = subCenter.moveSE
  val subSW = subCenter.moveSW

  val outlierNE = subCenter.moveN.moveNE
  val outlierNW = subCenter.moveN.moveNW
  val outlierSE = subCenter.moveS.moveSE
  val outlierSW = subCenter.moveS.moveSW
}
