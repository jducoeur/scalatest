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
package org.scalatest.concurrent

import org.scalatest.OptionValues._
import TimeLimits._
import org.scalatest.SharedHelpers.thisLineNumber
import java.io.ByteArrayInputStream
import java.net.SocketException
import java.net.ServerSocket
import java.net.Socket
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel
import org.scalatest.time._
import org.scalatest.{SeveredStackTraces, AsyncFunSpec, Resources}
import org.scalatest.exceptions.TestFailedException
import org.scalatest.exceptions.TestCanceledException
import org.scalatest.Retries._
import org.scalatest.tagobjects.Retryable
import org.scalatest.Matchers
import scala.concurrent.Future
import org.scalatest.Succeeded
import org.scalatest.Failed
import org.scalatest.Canceled
import org.scalatest.Pending
import org.scalatest.Outcome

import scala.util.{Try, Success, Failure}

class TimeLimitsSpec extends AsyncFunSpec with Matchers {

/*
  override def withFixture(test: NoArgTest) = {
    if (isRetryable(test))
      withRetry { super.withFixture(test) }
    else
      super.withFixture(test)
  }
*/

  describe("The failAfter construct") {

    describe("when work with T") {

      it("should blow up with TestFailedException when it times out", Retryable) {
        val caught = the[TestFailedException] thrownBy {
          failAfter(Span(100, Millis)) {
            SleepHelper.sleep(200)
          }
        }
        caught.message.value should be(Resources.timeoutFailedAfter("100 milliseconds"))
        caught.failedCodeFileName.value should be("TimeLimitsSpec.scala")
        caught.failedCodeLineNumber.value should equal(thisLineNumber - 6)
      }

      it("should pass normally when the timeout is not reached") {
        failAfter(Span(200, Millis)) {
          SleepHelper.sleep(100)
        }
        succeed
      }


      // SKIP-SCALATESTJS-START
      it("should blow up with TestFailedException when the task does not response interrupt request and pass after the timeout") {
        a[TestFailedException] should be thrownBy {
          failAfter(timeout = Span(100, Millis)) {
            for (i <- 1 to 10) {
              try {
                SleepHelper.sleep(50)
              }
              catch {
                case _: InterruptedException =>
                  Thread.interrupted() // Swallow the interrupt
              }
            }
          }
        }
      }
      // SKIP-SCALATESTJS-END

      it("should not catch exception thrown from the test") {
        an[InterruptedException] should be thrownBy {
          failAfter(Span(100, Millis)) {
            throw new InterruptedException
            succeed
          }
        }
      }

      // SKIP-SCALATESTJS-START
      it("should set the exception thrown from the test after timeout as cause of TestFailedException") {
        val caught = the[TestFailedException] thrownBy {
          failAfter(Span(100, Millis)) {
            for (i <- 1 to 10) {
              try {
                SleepHelper.sleep(50)
              }
              catch {
                case _: InterruptedException =>
                  Thread.interrupted() // Swallow the interrupt
              }
            }
            throw new IllegalArgumentException("Something went wrong!")
            succeed
          }
        }
        assert(caught.getCause().getClass === classOf[IllegalArgumentException])
      }

      it("should close a Socket connection via SocketSignaler when the timeout is reached") {
        val serverSocket = new ServerSocket(0)
        @volatile
        var drag = true
        val serverThread = new Thread() {
          override def run() {
            val clientSocket = serverSocket.accept()
            while (drag) {
              try {
                SleepHelper.sleep(100)
              }
              catch {
                case _: InterruptedException => Thread.interrupted()
              }
            }
            serverSocket.close()
          }
        }
        serverThread.start()
        val clientSocket = new Socket("localhost", serverSocket.getLocalPort())
        val inputStream = clientSocket.getInputStream()

        a[TestFailedException] should be thrownBy {
          failAfter(Span(100, Millis)) {
            inputStream.read()
          }(SocketSignaler(clientSocket))
        }
        clientSocket.close()
        drag = false
        succeed
      }

      it("should close a Socket connection via FunSignaler when the timeout is reached") {
        val serverSocket = new ServerSocket(0)
        @volatile
        var drag = true
        val serverThread = new Thread() {
          override def run() {
            val clientSocket = serverSocket.accept()
            while (drag) {
              try {
                SleepHelper.sleep(100)
              }
              catch {
                case _: InterruptedException => Thread.interrupted()
              }
            }
            serverSocket.close()
          }
        }
        serverThread.start()
        val clientSocket = new Socket("localhost", serverSocket.getLocalPort())
        val inputStream = clientSocket.getInputStream()

        a[TestFailedException] should be thrownBy {
          failAfter(Span(100, Millis)) {
            inputStream.read()
          }(Signaler { t => clientSocket.close() })
        }
        clientSocket.close()
        drag = false
        succeed
      }
      // SKIP-SCALATESTJS-END

      it("should wait for the test to finish when DoNotSignal.") {
        var x = 0
        val caught = the[TestFailedException] thrownBy {
          failAfter(Span(100, Millis)) {
            SleepHelper.sleep(200)
            x = 1
          }(DoNotSignal)
        }
        x should be(1)
      }

      // SKIP-SCALATESTJS-START
      it("should close a Selector connection via SelectorSignaler when the timeout is reached") {
        val selector = Selector.open()
        val ssChannel = ServerSocketChannel.open()
        ssChannel.configureBlocking(false)
        ssChannel.socket().bind(new InetSocketAddress(0))
        ssChannel.register(selector, SelectionKey.OP_ACCEPT)
        @volatile
        var drag = true
        val serverThread = new Thread() {
          override def run() {
            selector.select()
            val it = selector.selectedKeys.iterator
            while (it.hasNext) {
              val selKey = it.next().asInstanceOf[SelectionKey]
              it.remove()
              if (selKey.isAcceptable()) {
                val ssChannel = selKey.channel().asInstanceOf[ServerSocketChannel]
                while (drag) {
                  try {
                    SleepHelper.sleep(100)
                  }
                  catch {
                    case _: InterruptedException => Thread.interrupted()
                  }
                }
              }
            }
            ssChannel.close()
          }
        }

        val clientSelector = Selector.open();
        val sChannel = SocketChannel.open()
        sChannel.configureBlocking(false);
        sChannel.connect(new InetSocketAddress("localhost", ssChannel.socket().getLocalPort()));
        sChannel.register(selector, sChannel.validOps());

        a[TestFailedException] should be thrownBy {
          failAfter(Span(100, Millis)) {
            clientSelector.select()
          }(SelectorSignaler(clientSelector))
        }
        clientSelector.close()
        drag = false
        succeed
      }
      // SKIP-SCALATESTJS-END
    }

    describe("when work with Future[T]") {

      it("should blow up with TestFailedException when it times out in main block that create the Future", Retryable) {
        val caught = the[TestFailedException] thrownBy {
          failAfter(Span(100, Millis)) {
            SleepHelper.sleep(200)
            Future.successful(Success("test"))
          }
        }
        caught.message.value should be(Resources.timeoutFailedAfter("100 milliseconds"))
        caught.failedCodeFileName.value should be("TimeLimitsSpec.scala")
        caught.failedCodeLineNumber.value should equal(thisLineNumber - 7)
      }

      it("should blow up with TestFailedException when it times out in the Future that gets returned", Retryable) {
        val futureException =
          recoverToExceptionIf[TestFailedException](
            failAfter(Span(100, Millis)) {
              Future {
                SleepHelper.sleep(200)
                Success("test")
              }
            }
          )
        futureException map { caught =>
          caught.message.value should be (Resources.timeoutFailedAfter("100 milliseconds"))
          caught.failedCodeFileName.value should be("TimeLimitsSpec.scala")
          caught.failedCodeLineNumber.value should equal(thisLineNumber - 10)
        }
      }

      it("should pass normally when the timeout is not reached in main block that create the future") {
        failAfter(Span(200, Millis)) {
          SleepHelper.sleep(100)
          Future.successful(Success("test"))
        }
        succeed
      }

      it("should pass normally when the timeout is not reached in main block that create the future and in the future itself") {
        failAfter(Span(200, Millis)) {
          Future {
            SleepHelper.sleep(100)
            Success("test")
          }
        }
        succeed
      }

      it("should not catch exception thrown from the main block that create the future") {
        an[InterruptedException] should be thrownBy {
          failAfter(Span(100, Millis)) {
            throw new InterruptedException
            Future.successful(Success("test"))
          }
        }
        succeed
      }

      /*it("should not catch exception thrown from the future block") {
        implicit val globalExecContext = scala.concurrent.ExecutionContext.Implicits.global
        //val futureOfException: Future[InterruptedException] = recoverToExceptionIf[InterruptedException] {
          failAfter(Span(100, Millis)) {
            Future {
              throw new InterruptedException
              Success("test")
            }
          }
        //}
        //println("###here: " + futureOfException)
        /*futureOfException.map { caught =>
          succeed
        }*/
        //futureOfException.map { caught =>
          succeed
        //}
      }*/

      it("should wait for the test to finish when DoNotSignal.") {
        var x = 0
        val caught = the[TestFailedException] thrownBy {
          failAfter(Span(100, Millis)) {
            SleepHelper.sleep(200)
            x = 1
            Success("test")
          }(DoNotSignal)
        }
        x should be (1)
      }
    }
  }

  describe("The cancelAfter construct") {

    it("should blow up with TestCanceledException when it times out") {
      val caught = the [TestCanceledException] thrownBy {
        cancelAfter(Span(1000, Millis)) {
          SleepHelper.sleep(2000)
        }
      }
      caught.message.value should be (Resources.timeoutCanceledAfter("1000 milliseconds"))
      caught.failedCodeLineNumber.value should equal (thisLineNumber - 5)
      caught.failedCodeFileName.value should be ("TimeLimitsSpec.scala")
    }
    
    it("should pass normally when timeout is not reached") {
      cancelAfter(Span(2000, Millis)) {
        SleepHelper.sleep(1000)
      }
      succeed
    }
    
    it("should blow up with TestCanceledException when the task does not response interrupt request and pass after the timeout") {
      a [TestCanceledException] should be thrownBy {
        cancelAfter(timeout = Span(1000, Millis)) {
          for (i <- 1 to 10) {
            try {
              SleepHelper.sleep(500)
            }
            catch {
              case _: InterruptedException =>
                Thread.interrupted() // Swallow the interrupt
            }
          }
        }
      }
    }
    
    it("should not catch exception thrown from the test") {
      an [InterruptedException] should be thrownBy {
        cancelAfter(Span(1000, Millis)) {
          throw new InterruptedException
          succeed
        }
      }
    }
    
    it("should set exception thrown from the test after timeout as cause of TestCanceledException") {
      val caught = the [TestCanceledException] thrownBy {
        cancelAfter(Span(1000, Millis)) {
          for (i <- 1 to 10) {
            try {
              SleepHelper.sleep(500)
            }
            catch {
              case _: InterruptedException =>
                Thread.interrupted() // Swallow the interrupt
            }
          }
          throw new IllegalArgumentException("Something goes wrong!")
          succeed
        }
      }
      assert(caught.getCause().getClass === classOf[IllegalArgumentException])
    }

    // SKIP-SCALATESTJS-START
    it("should close Socket connection via SocketSignaler when timeout reached") {
      val serverSocket = new ServerSocket(0)
      @volatile
      var drag = true
      val serverThread = new Thread() {
        override def run() {
          val clientSocket = serverSocket.accept()
          while(drag) {
            try {
              SleepHelper.sleep(1000)
            }
            catch {
              case _: InterruptedException => Thread.interrupted()
            }
          }
          serverSocket.close()
        }
      }
      serverThread.start()
      val clientSocket = new Socket("localhost", serverSocket.getLocalPort())
      val inputStream = clientSocket.getInputStream()
      
      val caught = the [TestCanceledException] thrownBy {
        cancelAfter(Span(1000, Millis)) {
          inputStream.read()
        } (SocketSignaler(clientSocket))
      }
      clientSocket.close()
      drag = false
      succeed // TODO: Chee Seng, why is caught captured? It isn't used.
    }
    
    it("should close Socket connection via FunSignaler when timeout reached") {
      val serverSocket = new ServerSocket(0)
      @volatile
      var drag = true
      val serverThread = new Thread() {
        override def run() {
          val clientSocket = serverSocket.accept()
          while(drag) {
            try {
              SleepHelper.sleep(1000)
            }
            catch {
              case _: InterruptedException => Thread.interrupted()
            }
          }
          serverSocket.close()
        }
      }
      serverThread.start()
      val clientSocket = new Socket("localhost", serverSocket.getLocalPort())
      val inputStream = clientSocket.getInputStream()
      
      a [TestCanceledException] should be thrownBy {
        cancelAfter(Span(1000, Millis)) {
          inputStream.read()
        } (Signaler { t => clientSocket.close() } )
      }
      clientSocket.close()
      drag = false
      succeed
    }
    // SKIP-SCALATESTJS-END
    
    it("should wait for the test to finish when DoNotSignal is used.") {
      var x = 0
      val caught = the [TestCanceledException] thrownBy {
        cancelAfter(Span(1000, Millis)) {
          SleepHelper.sleep(2000)
          x = 1
        } (DoNotSignal)
      }
      x should be (1)
    }

    // SKIP-SCALATESTJS-START
    it("should close Selector connection via SelectorSignaler when timeout reached") {
      val selector = Selector.open()
      val ssChannel = ServerSocketChannel.open()
      ssChannel.configureBlocking(false)
      ssChannel.socket().bind(new InetSocketAddress(0))
      ssChannel.register(selector, SelectionKey.OP_ACCEPT)
      @volatile
      var drag = true
      val serverThread = new Thread() {
        override def run() {
          selector.select()
          val it = selector.selectedKeys.iterator
          while (it.hasNext) {
            val selKey = it.next().asInstanceOf[SelectionKey]
            it.remove()
            if (selKey.isAcceptable()) {
              val ssChannel = selKey.channel().asInstanceOf[ServerSocketChannel]
              while(drag) {
                try {
                  SleepHelper.sleep(1000)
                }
                catch {
                  case _: InterruptedException => Thread.interrupted()
                }
              }
            }
          }
          ssChannel.close()
        }
      }
    
      val clientSelector = Selector.open();
      val sChannel = SocketChannel.open()
      sChannel.configureBlocking(false);
      sChannel.connect(new InetSocketAddress("localhost", ssChannel.socket().getLocalPort()));
      sChannel.register(selector, sChannel.validOps());
    
      a [TestCanceledException] should be thrownBy {
        cancelAfter(Span(1000, Millis)) {
          clientSelector.select()
        } (SelectorSignaler(clientSelector))
      }
      clientSelector.close()
      drag = false
      succeed
    }
    // SKIP-SCALATESTJS-END
  }
}
