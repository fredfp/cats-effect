/*
 * Copyright 2020-2021 Typelevel
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

package cats.effect.unsafe

import java.util.concurrent.{Executors, ScheduledExecutorService}

import scala.concurrent.duration.FiniteDuration

private[unsafe] abstract class SchedulerCompanionPlatform { this: Scheduler.type =>
  def createDefaultScheduler(): (Scheduler, () => Unit) = {
    val scheduler = Executors.newSingleThreadScheduledExecutor { r =>
      val t = new Thread(r)
      t.setName("io-scheduler")
      t.setDaemon(true)
      t.setPriority(Thread.MAX_PRIORITY)
      t
    }
    (Scheduler.fromScheduledExecutor(scheduler), { () => scheduler.shutdown() })
  }

  def fromScheduledExecutor(scheduler: ScheduledExecutorService): Scheduler =
    new Scheduler {
      def sleep(delay: FiniteDuration, task: Runnable): Runnable = {
        val future = scheduler.schedule(task, delay.length, delay.unit)

        { () =>
          future.cancel(false)
          ()
        }
      }

      def nowMillis() = System.currentTimeMillis()

      def monotonicNanos() = System.nanoTime()
    }
}
