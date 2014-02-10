/*
 * Copyright 2001-2013 Artima, Inc.
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
package org.scalautils

import java.text.MessageFormat

trait Fact {

  private def makeString(rawString: String, args: IndexedSeq[Any]): String = {
    val msgFmt = new MessageFormat(rawString)
    msgFmt.format(args.map(Prettifier.default).toArray)
  }

  // Or defs instead of lazy vals
  def failureMessage: String =
    if (failureMessageArgs.isEmpty) rawFailureMessage else makeString(rawFailureMessage, failureMessageArgs)

  def negatedFailureMessage: String =
    if (negatedFailureMessageArgs.isEmpty) rawNegatedFailureMessage else makeString(rawNegatedFailureMessage, negatedFailureMessageArgs)

  def midSentenceFailureMessage: String =
    if (midSentenceFailureMessageArgs.isEmpty) rawMidSentenceFailureMessage else makeString(rawMidSentenceFailureMessage, midSentenceFailureMessageArgs)

  def midSentenceNegatedFailureMessage: String =
    if (midSentenceNegatedFailureMessageArgs.isEmpty) rawMidSentenceNegatedFailureMessage else makeString(rawMidSentenceNegatedFailureMessage, midSentenceNegatedFailureMessageArgs)

  def value: Boolean

  def rawFailureMessage: String
  def rawNegatedFailureMessage: String
  def rawMidSentenceFailureMessage: String
  def rawMidSentenceNegatedFailureMessage: String

  def failureMessageArgs: IndexedSeq[Any]
  def negatedFailureMessageArgs: IndexedSeq[Any]
  def midSentenceFailureMessageArgs: IndexedSeq[Any]
  def midSentenceNegatedFailureMessageArgs: IndexedSeq[Any]

  def &&(fact: => Fact): Fact =
    if (value) {
        val myValue = value
        new Fact {

          lazy val value: Boolean = myValue && fact.value

          def rawFailureMessage: String = Resources("commaBut")
          def rawNegatedFailureMessage: String = Resources("commaAnd")
          def rawMidSentenceFailureMessage: String = Resources("commaBut")
          def rawMidSentenceNegatedFailureMessage: String = Resources("commaAnd")

          def failureMessageArgs = Vector(this.negatedFailureMessage, fact.midSentenceFailureMessage)
          def negatedFailureMessageArgs = Vector(this.negatedFailureMessage, fact.midSentenceNegatedFailureMessage)
          def midSentenceFailureMessageArgs = Vector(this.midSentenceNegatedFailureMessage, fact.midSentenceFailureMessage)
          def midSentenceNegatedFailureMessageArgs = Vector(this.midSentenceNegatedFailureMessage, fact.midSentenceNegatedFailureMessage)
        }
      }
    else
      this

  def &(fact: => Fact): Fact = &&(fact)

  def ||(fact: => Fact): Fact = {
    def myValue = this.value // by-name to be lazy
    new Fact {

      lazy val value: Boolean = myValue || fact.value

      def rawFailureMessage: String = Resources("commaAnd")
      def rawNegatedFailureMessage: String = Resources("commaAnd")
      def rawMidSentenceFailureMessage: String = Resources("commaAnd")
      def rawMidSentenceNegatedFailureMessage: String = Resources("commaAnd")

      def failureMessageArgs = Vector(this.failureMessage, fact.midSentenceFailureMessage)
      def negatedFailureMessageArgs = Vector(this.failureMessage, fact.midSentenceNegatedFailureMessage)
      def midSentenceFailureMessageArgs = Vector(this.midSentenceFailureMessage, fact.midSentenceFailureMessage)
      def midSentenceNegatedFailureMessageArgs = Vector(this.midSentenceFailureMessage, fact.midSentenceNegatedFailureMessage)
    }
  }

  def |(fact: => Fact): Fact = ||(fact)

  def unary_! : Fact =
    new Fact {

      lazy val value: Boolean = !this.value

      def rawFailureMessage: String = this.rawNegatedFailureMessage
      def rawNegatedFailureMessage: String = this.rawFailureMessage
      def rawMidSentenceFailureMessage: String = this.rawMidSentenceNegatedFailureMessage
      def rawMidSentenceNegatedFailureMessage: String = this.rawMidSentenceFailureMessage

      def failureMessageArgs: IndexedSeq[Any] = this.negatedFailureMessageArgs
      def negatedFailureMessageArgs: IndexedSeq[Any] = this.failureMessageArgs
      def midSentenceFailureMessageArgs: IndexedSeq[Any] = this.midSentenceNegatedFailureMessageArgs
      def midSentenceNegatedFailureMessageArgs: IndexedSeq[Any] = this.midSentenceFailureMessageArgs
    }

  /*
  def &&(bool: => Boolean): Fact = ...
  def ||(bool: => Boolean): Fact = ...
  */
}

class SimpleMacroFact(expression: => Boolean, expressionText: String) extends Fact {

  lazy val value: Boolean = expression

  def rawFailureMessage: String = Resources("wasFalse")
  def rawNegatedFailureMessage: String = Resources("wasTrue")
  def rawMidSentenceFailureMessage: String = Resources("wasFalse")
  def rawMidSentenceNegatedFailureMessage: String = Resources("wasTrue")

  def failureMessageArgs: IndexedSeq[Any] = Vector(UnquotedString(expressionText))
  def negatedFailureMessageArgs: IndexedSeq[Any] = Vector(UnquotedString(expressionText))
  def midSentenceFailureMessageArgs: IndexedSeq[Any] = Vector(UnquotedString(expressionText))
  def midSentenceNegatedFailureMessageArgs: IndexedSeq[Any] = Vector(UnquotedString(expressionText))

}

class NotMacroFact(fact: Fact) extends Fact {

  lazy val value: Boolean = !fact.value

  def rawFailureMessage: String = fact.rawNegatedFailureMessage
  def rawNegatedFailureMessage: String = fact.rawFailureMessage
  def rawMidSentenceFailureMessage: String = fact.rawMidSentenceNegatedFailureMessage
  def rawMidSentenceNegatedFailureMessage: String = fact.rawMidSentenceFailureMessage

  def failureMessageArgs: IndexedSeq[Any] = fact.negatedFailureMessageArgs
  def negatedFailureMessageArgs: IndexedSeq[Any] = fact.failureMessageArgs
  def midSentenceFailureMessageArgs: IndexedSeq[Any] = fact.midSentenceNegatedFailureMessageArgs
  def midSentenceNegatedFailureMessageArgs: IndexedSeq[Any] = fact.midSentenceFailureMessageArgs
}

class BinaryMacroFact(left: Any, operator: String, right: Any, expression: => Boolean) extends Fact {

  def this(left: Any, operator: String, right: Any, fact: Fact) =
    this(left, operator, right, fact.value)

  lazy val value: Boolean = expression

  def getObjectsForFailureMessage =
    left match {
      case aEqualizer: org.scalautils.TripleEqualsSupport#Equalizer[_] =>
        Prettifier.getObjectsForFailureMessage(aEqualizer.leftSide, right)
      case aEqualizer: org.scalautils.TripleEqualsSupport#CheckingEqualizer[_] =>
        Prettifier.getObjectsForFailureMessage(aEqualizer.leftSide, right)
      case _ => Prettifier.getObjectsForFailureMessage(left, right)
    }

  def rawFailureMessage: String =
    operator match {
      case "==" => Resources("didNotEqual")
      case "===" => Resources("didNotEqual")
      case "!=" => Resources("equaled")
      case "!==" => Resources("equaled")
      case ">" => Resources("wasNotGreaterThan")
      case ">=" => Resources("wasNotGreaterThanOrEqualTo")
      case "<" => Resources("wasNotLessThan")
      case "<=" => Resources("wasNotLessThanOrEqualTo")
      case "&&" | "&" =>
        (left, right) match {
          case (leftFact: Fact, rightFact: Fact) =>
            if (leftFact.value)
              Resources("commaBut")
            else
              leftFact.rawFailureMessage
          case (leftFact: Fact, rightAny: Any) =>
            if (leftFact.value)
              Resources("commaBut")
            else
              leftFact.rawFailureMessage
          case _ =>
            Resources("commaBut")
        }
      case "||" | "|" => Resources("commaAnd")
      case _ => Resources("expressionWasFalse")
    }

  def rawNegatedFailureMessage: String =
    operator match {
      case "==" => Resources("equaled")
      case "===" => Resources("equaled")
      case "!=" => Resources("didNotEqual")
      case "!==" => Resources("didNotEqual")
      case ">" => Resources("wasGreaterThan")
      case ">=" => Resources("wasGreaterThanOrEqualTo")
      case "<" => Resources("wasLessThan")
      case "<=" => Resources("wasLessThanOrEqualTo")
      case "&&" | "&" => Resources("commaAnd")
      case "||" | "|" => Resources("commaAnd")
      case _ => Resources("expressionWasTrue")
    }

  def rawMidSentenceFailureMessage: String = rawFailureMessage
  def rawMidSentenceNegatedFailureMessage: String = rawNegatedFailureMessage

  def failureMessageArgs: IndexedSeq[Any] =
    operator match {
      case "==" | "===" | "!=" | "!==" | ">" | ">=" | "<" | "<=" =>
        val (leftee, rightee) = getObjectsForFailureMessage
        Vector(leftee, rightee)
      case "&&" | "&" =>
        (left, right) match {
          case (leftFact: Fact, rightFact: Fact) =>
            if (leftFact.value)
              Vector(UnquotedString(leftFact.negatedFailureMessage), UnquotedString(rightFact.midSentenceFailureMessage))
            else
              leftFact.failureMessageArgs
          case (leftFact: Fact, rightAny: Any) =>
            if (leftFact.value)
              Vector(UnquotedString(leftFact.negatedFailureMessage), rightAny)
            else
              leftFact.failureMessageArgs
          case (leftAny: Any, rightFact: Fact) =>
            Vector(leftAny, UnquotedString(if (rightFact.value) rightFact.midSentenceNegatedFailureMessage else rightFact.midSentenceFailureMessage))
          case _ =>
            Vector(left, right)
        }
      case "||" | "|" =>
        (left, right) match {
          case (leftFact: Fact, rightFact: Fact) =>
            Vector(UnquotedString(leftFact.failureMessage), UnquotedString(rightFact.midSentenceFailureMessage))
          case (leftFact: Fact, rightAny: Any) =>
            Vector(UnquotedString(leftFact.failureMessage), rightAny)
          case (leftAny: Any, rightFact: Fact) =>
            Vector(leftAny, UnquotedString(rightFact.midSentenceFailureMessage))
          case _ =>
            Vector(left, right)
        }
      case _ => Vector.empty
    }

  def negatedFailureMessageArgs: IndexedSeq[Any] =
    operator match {
      case "==" | "===" | "!=" | "!==" | ">" | ">=" | "<" | "<=" =>
        val (leftee, rightee) = getObjectsForFailureMessage
        Vector(leftee, rightee)
      case "&&" | "&" =>
        (left, right) match {
          case (leftFact: Fact, rightFact: Fact) =>
            Vector(
              UnquotedString(if (leftFact.value) leftFact.negatedFailureMessage else leftFact.failureMessage),
              UnquotedString(if (rightFact.value) rightFact.midSentenceNegatedFailureMessage else rightFact.midSentenceFailureMessage)
            )
          case (leftFact: Fact, rightAny: Any) =>
            Vector(UnquotedString(if (leftFact.value) leftFact.negatedFailureMessage else leftFact.failureMessage), rightAny)
          case (leftAny: Any, rightFact: Fact) =>
            Vector(leftAny, UnquotedString(if (rightFact.value) rightFact.midSentenceNegatedFailureMessage else rightFact.negatedFailureMessage))
          case _ =>
            Vector(left, right)
        }
      case "||" | "|" =>
        (left, right) match {
          case (leftFact: Fact, rightFact: Fact) =>
            Vector(
              UnquotedString(if (leftFact.value) leftFact.negatedFailureMessage else leftFact.failureMessage),
              UnquotedString(if (rightFact.value) rightFact.midSentenceNegatedFailureMessage else rightFact.midSentenceFailureMessage)
            )
          case (leftFact: Fact, rightAny: Any) =>
            Vector(UnquotedString(if (leftFact.value) leftFact.negatedFailureMessage else leftFact.failureMessage), rightAny)
          case (leftAny: Any, rightFact: Fact) =>
            Vector(leftAny, UnquotedString(if (rightFact.value) rightFact.midSentenceNegatedFailureMessage else rightFact.midSentenceFailureMessage))
          case _ =>
            Vector(left, right)
        }
      case _ => Vector.empty
    }

  def midSentenceFailureMessageArgs: IndexedSeq[Any] = failureMessageArgs
  def midSentenceNegatedFailureMessageArgs: IndexedSeq[Any] = negatedFailureMessageArgs

}

//case class Yes extends Fact

//case class No extends Fact