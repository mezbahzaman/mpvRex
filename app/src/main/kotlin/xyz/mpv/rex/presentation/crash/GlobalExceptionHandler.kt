package xyz.mpv.rex.presentation.crash

import android.content.Context
import android.content.Intent
import kotlin.system.exitProcess
import xyz.mpv.rex.ui.player.StremioHandoff
import xyz.mpv.rex.ui.player.StremioProgressSnapshot

class GlobalExceptionHandler(
  private val context: Context,
  private val activity: Class<*>,
) : Thread.UncaughtExceptionHandler {
  override fun uncaughtException(
    t: Thread,
    e: Throwable,
  ) {
    val snapshot = StremioProgressSnapshot.consume(context)
    if (snapshot.third) {
      context.sendBroadcast(StremioHandoff.resultIntent(snapshot.first, snapshot.second))
    }
    val intent = Intent(context, activity)
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
    intent.putExtra("exception", e.stackTraceToString())
    context.startActivity(intent)
    exitProcess(0)
  }
}
