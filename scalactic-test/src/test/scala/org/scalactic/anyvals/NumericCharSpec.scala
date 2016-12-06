/*
 * Copyright 2001-2014 Artima, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scalactic.anyvals

import org.scalatest._
import scala.collection.mutable.WrappedArray
import OptionValues._
//import org.scalactic.StrictCheckedEquality

class NumericCharSpec extends FunSpec with Matchers/* with StrictCheckedEquality*/ {
  describe("A NumericChar") {
    describe("should offer a from factory method that") {
      it("returns Some[NumericChar] if the passed Char is between '0' and '9'") {
        NumericChar.from('0').value.value shouldBe '0'
        NumericChar.from('5').value.value shouldBe '5'
        NumericChar.from('9').value.value shouldBe '9'
      }
      it("returns None if the passed Char is NOT between '0' and '9'") {
        NumericChar.from('a') shouldBe None
        NumericChar.from('z') shouldBe None
        NumericChar.from('A') shouldBe None
        NumericChar.from(0) shouldBe None
        NumericChar.from(-1.toChar) shouldBe None
      }
    } 
    describe("should offer an ensuringValid factory method that") {
      it("returns NumericChar if the passed Char is between '0' and '9'") {
        NumericChar.ensuringValid('0').value shouldBe '0'
        NumericChar.ensuringValid('5').value shouldBe '5'
        NumericChar.ensuringValid('9').value shouldBe '9'
      }
      it("throws AssertionError if the passed Char is NOT between '0' and '9'") {
        an [AssertionError] should be thrownBy NumericChar.ensuringValid('a')
        an [AssertionError] should be thrownBy NumericChar.ensuringValid('z')
        an [AssertionError] should be thrownBy NumericChar.ensuringValid('A')
        an [AssertionError] should be thrownBy NumericChar.ensuringValid(0)
        an [AssertionError] should be thrownBy NumericChar.ensuringValid(-1.toChar)
      }
    } 
    it("should define min and max values") {
      NumericChar.MinValue shouldBe '0'
      NumericChar.MaxValue shouldBe '9'
    } 
    it("should define min and max methods") {
      NumericChar('0') min NumericChar('1') shouldBe NumericChar('0')
      NumericChar('0') max NumericChar('1') shouldBe NumericChar('1')
      NumericChar('8') min NumericChar('9') shouldBe NumericChar('8')
      NumericChar('8') max NumericChar('9') shouldBe NumericChar('9')
    } 
    it("should define methods to convert to the numeric value the character represents") {
      NumericChar('0').asDigit shouldBe 0
      NumericChar('9').asDigit shouldBe 9
      NumericChar('1').asDigitPosInt shouldBe PosInt(1)
      NumericChar('9').asDigitPosInt shouldBe PosInt(9)
      NumericChar('0').asDigitPosZInt shouldBe PosZInt(0)
      NumericChar('9').asDigitPosZInt shouldBe PosZInt(9)
    } 
    it("should have a pretty toString") {
      NumericChar.from('0').value.toString shouldBe "NumericChar(0)"
      NumericChar.from('9').value.toString shouldBe "NumericChar(9)"
    }
    it("should return the same type from its unary_+ method") {
      +NumericChar('3') shouldEqual NumericChar('3')
    } 
    it("should be automatically widened to compatible AnyVal targets") {
      (NumericChar('3'): Int) shouldEqual '3'.toInt
      (NumericChar('3'): Long) shouldEqual '3'.toLong
      (NumericChar('3'): Float) shouldEqual '3'.toFloat
      (NumericChar('3'): Double) shouldEqual '3'.toDouble

      (NumericChar('3'): PosInt) shouldEqual PosInt.from('3'.toInt).get
      (NumericChar('3'): PosLong) shouldEqual PosLong.from('3'.toLong).get
      (NumericChar('3'): PosFloat) shouldEqual PosFloat.from('3'.toFloat).get
      (NumericChar('3'): PosDouble) shouldEqual PosDouble.from('3'.toDouble).get

      (NumericChar('3'): PosZInt) shouldEqual PosZInt.from('3'.toInt).get
      (NumericChar('3'): PosZLong) shouldEqual PosZLong.from('3'.toLong).get
      (NumericChar('3'): PosZFloat) shouldEqual PosZFloat.from('3'.toFloat).get
      (NumericChar('3'): PosZDouble) shouldEqual PosZDouble.from('3'.toDouble).get
    }
    describe("when a compatible AnyVal is passed to a + method invoked on it") {
      it("should give the same AnyVal type back at compile time, and correct value at runtime") {
        // When adding a "primitive"
        val opInt = NumericChar('3') + 3
        opInt shouldEqual '3'.toInt + 3

        val opLong = NumericChar('3') + 3L
        opLong shouldEqual '3'.toLong + 3L

        val opFloat = NumericChar('3') + 3.0F
        opFloat shouldEqual '3'.toFloat + 3.0F

        val opDouble = NumericChar('3') + 3.0
        opDouble shouldEqual '3'.toDouble + 3.0

        // When adding a Pos*
        val opPosInt = NumericChar('3') + PosInt(3)
        opPosInt shouldEqual '3'.toInt + 3

        val opPosLong = NumericChar('3') + PosLong(3L)
        opPosLong shouldEqual '3'.toInt + 3L

        val opPosFloat = NumericChar('3') + PosFloat(3.0F)
        opPosFloat shouldEqual '3'.toInt + 3.0F

        val opPosDouble = NumericChar('3') + PosDouble(3.0)
        opPosDouble shouldEqual '3'.toInt + 3.0

        // When adding a *PosZ
        val opPosZ = NumericChar('3') + PosZInt(3)
        opPosZ shouldEqual '3'.toInt + 3

        val opPosZLong = NumericChar('3') + PosZLong(3L)
        opPosZLong shouldEqual '3'.toInt + 3L

        val opPosZFloat = NumericChar('3') + PosZFloat(3.0F)
        opPosZFloat shouldEqual '3'.toInt + 3.0F

        val opPosZDouble = NumericChar('3') + PosZDouble(3.0)
        opPosZDouble shouldEqual '3'.toInt + 3.0
      }
    }

    describe("when created with apply method") {

      it("should compile when '8' is passed in") {
        "NumericChar('8')" should compile
        NumericChar('8').value shouldEqual '8'
      }

      it("should not compile when 'A' is passed in") {
        "NumericChar('A')" shouldNot compile
      }

      it("should not compile when -8 is passed in") {
        "NumericChar(-8.toChar)" shouldNot compile
      }

      it("should not compile when x is passed in") {
        val x: Char = 'A'
        "NumericChar(x)" shouldNot compile
      }
    }
    describe("when specified as a plain-old Char") {

      def takesNumericChar(dig: NumericChar): Char = dig.value

      it("should compile when '8' is passed in") {
        "takesNumericChar('8')" should compile
        takesNumericChar('8') shouldEqual '8'
      }

      it("should not compile when 'x' is passed in") {
        "takesNumericChar('x')" shouldNot compile
      }

      it("should not compile when -8 is passed in") {
        "takesNumericChar(-8.toChar)" shouldNot compile
      }

      it("should not compile when x is passed in") {
        val x: Int = 'x'
        "takesNumericChar(x)" shouldNot compile
      }
    }
  }
}

