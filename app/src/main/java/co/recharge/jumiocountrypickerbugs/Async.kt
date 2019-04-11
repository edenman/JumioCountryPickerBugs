package co.recharge.jumiocountrypickerbugs

import rx.Observable
import rx.Scheduler
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS

/**
 * Helper methods inspired by https://github.com/duemunk/Async but with RxJava under the hood.
 */
class Async {
  companion object {
    fun main(after: Int = 0, unit: TimeUnit = SECONDS, task: () -> Unit) {
      enqueue(task, on = AndroidSchedulers.mainThread(), after = after, unit = unit)
    }

    fun io(after: Int = 0, unit: TimeUnit = SECONDS, task: () -> Unit) {
      enqueue(task, on = Schedulers.io(), after = after, unit = unit)
    }

    private fun enqueue(task: () -> Unit, on: Scheduler?, after: Int, unit: TimeUnit) {
      Observable.fromCallable(task)
          .delaySubscription(after.toLong(), unit, on)
          .subscribeOn(on)
          .subscribe()
    }
  }
}
