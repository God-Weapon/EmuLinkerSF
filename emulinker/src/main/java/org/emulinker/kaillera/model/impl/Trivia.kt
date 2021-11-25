package org.emulinker.kaillera.model.impl

import java.io.*
import java.lang.Exception
import java.lang.Runnable
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

class Trivia(private val server: KailleraServerImpl) : Runnable {

  private var exitThread = false
  private var triviaPaused = false
  private var newQuestion = true
  private var answer: String? = null
  private var hint: CharArray? = null
  private var hint1 = false
  private var hint2 = false
  var isAnswered = false
    private set
  private var questionsCount = 0
  private var ipStreak = ""
  private var scoreStreak = 0
  private var questionTime = 30000
  private val questions: MutableList<Questions> = ArrayList()
  private val questionsNum: MutableList<Int> = ArrayList()
  private val scores: MutableList<Scores> = ArrayList()
  fun setQuestionTime(questionTime: Int) {
    this.questionTime = questionTime
  }

  fun getScores(): List<Scores> {
    return scores
  }

  fun setTriviaPaused(triviaPaused: Boolean) {
    this.triviaPaused = triviaPaused
  }

  inner class Scores(var nick: String, var iP: String, var score: Int)
  private inner class Questions(var question: String, var answer: String)

  override fun run() {
    var count = 0
    var temp: Int
    val generator = Random()
    if (questions.size > 1) {
      temp = generator.nextInt(questionsNum.size - 1)
      questionsCount = questionsNum[temp]
      questionsNum.removeAt(temp)
    }
    try {
      Thread.sleep(10000)
    } catch (e: Exception) {}
    while (!exitThread) {
      if (!triviaPaused) {
        if (newQuestion) {
          count++
          if (count % 15 == 0) {
            saveScores(false)
            displayHighScores(false)
          }
          newQuestion = false
          hint1 = true
          hint2 = false
          server.announce("<Trivia> " + questions[questionsCount].question, false, null)
          if (!isAnswered)
              try {
                Thread.sleep(10000)
              } catch (e: Exception) {}
          if (!isAnswered) {
            server.announce("<Trivia> " + "35 seconds left...", false, null)
            try {
              Thread.sleep(5000)
            } catch (e: Exception) {}
          }
        }
        if (hint1 && !isAnswered) {
          newQuestion = false
          hint1 = false
          hint2 = true
          answer = questions[questionsCount].answer.lowercase(Locale.getDefault())
          answer = answer!!.replace(" ", "    ")
          hint = answer!!.toCharArray()
          for (w in hint!!.indices) {
            if ((w + 1) % 2 == 0 && hint!![w] != ' ') {
              hint!![w] = '_'
            }
          }
          answer = String(hint!!)
          answer = answer!!.replace("_", " _ ")
          server.announce("<Trivia> Hint1: $answer", false, null)
          if (!isAnswered)
              try {
                Thread.sleep(10000)
              } catch (e: Exception) {}
          if (!isAnswered) {
            server.announce("<Trivia> " + "20 seconds left...", false, null)
            try {
              Thread.sleep(5000)
            } catch (e: Exception) {}
          }
        }
        if (hint2 && !isAnswered) {
          newQuestion = false
          hint1 = false
          hint2 = false
          answer = questions[questionsCount].answer.lowercase(Locale.getDefault())
          answer = answer!!.replace(" ", "    ")
          hint = answer!!.toCharArray()
          for (w in hint!!.indices) {
            if ((w + 1) % 4 == 0 && hint!![w] != ' ') {
              hint!![w] = '_'
            }
          }
          answer = String(hint!!)
          answer = answer!!.replace("_", " _ ")
          server.announce("<Trivia> Hint2: $answer", false, null)
          if (!isAnswered)
              try {
                Thread.sleep(10000)
              } catch (e: Exception) {}
          if (!isAnswered) {
            server.announce("<Trivia> " + "5 seconds left...", false, null)
            try {
              Thread.sleep(5000)
            } catch (e: Exception) {}
          }
        }
        if (!isAnswered) {
          server.announce(
              "<Trivia> " + "Time's up! The answer is: " + questions[questionsCount].answer,
              false,
              null)
        }
        if (count == questions.size) {
          count = 0
          server.announce(
              "<Trivia> " + "***All questions have been exhaused! Restarting list...***",
              false,
              null)
        }

        // Find questions not repeated
        if (questionsNum.size == 1) {
          questionsCount = questionsNum[0]
          questionsNum.clear()
          for (w in questions.indices) {
            questionsNum.add(w)
          }
        } else {
          temp = generator.nextInt(questionsNum.size - 1)
          questionsCount = questionsNum[temp]
          questionsNum.removeAt(temp)
        }
        try {
          Thread.sleep(5000)
        } catch (e: Exception) {}
        server.announce(
            "<Trivia> " +
                questionTime / 1000 +
                " seconds until the next question. Get ready for question " +
                (count + 1) +
                " of " +
                questions.size,
            false,
            null)
        try {
          Thread.sleep(questionTime.toLong())
        } catch (e: Exception) {}
        newQuestion = true
        hint1 = false
        hint2 = false
        isAnswered = false
      } else {
        try {
          Thread.sleep(1000)
        } catch (e: Exception) {}
      }
    }
  }

  fun isCorrect(message: String): Boolean {
    var message = message
    val numbers0 =
        arrayOf(
            "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten")
    val numbers1 = arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
    val placement0 =
        arrayOf(
            "first",
            "second",
            "third",
            "fourth",
            "fifth",
            "sixth",
            "seventh",
            "eighth",
            "nineth",
            "tenth")
    val placement1 = arrayOf("1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th")
    if (message.lowercase(Locale.getDefault()) ==
        questions[questionsCount].answer.lowercase(Locale.getDefault())) {
      isAnswered = true
      return true
    } else {
      for (i in numbers0.indices) {
        if (questions[questionsCount].answer.lowercase(Locale.getDefault()).contains(numbers0[i])) {
          message = message.replace(numbers1[i], numbers0[i])
        } else if (questions[questionsCount]
            .answer
            .lowercase(Locale.getDefault())
            .contains(numbers1[i])) {
          message = message.replace(numbers0[i], numbers1[i])
        }
      }
      if (message.lowercase(Locale.getDefault()) ==
          questions[questionsCount].answer.lowercase(Locale.getDefault())) {
        isAnswered = true
        return true
      } else {
        for (i in placement0.indices) {
          if (questions[questionsCount]
              .answer
              .lowercase(Locale.getDefault())
              .contains(placement0[i])) {
            message = message.replace(placement1[i], placement0[i])
          } else if (questions[questionsCount]
              .answer
              .lowercase(Locale.getDefault())
              .contains(placement1[i])) {
            message = message.replace(placement0[i], placement1[i])
          }
        }
        if (message.lowercase(Locale.getDefault()) ==
            questions[questionsCount].answer.lowercase(Locale.getDefault())) {
          isAnswered = true
          return true
        }
      }
    }
    return false
  }

  fun updateIP(ip: String, ip_update: String): Boolean {
    for (i in scores.indices) {
      if (scores[i].iP == ip) {
        scores[i].iP = ip_update
        return true
      }
    }
    return false
  }

  fun addScore(nick: String, ip: String, answer: String) {
    isAnswered = true
    for (i in scores.indices) {
      if (scores[i].iP == ip) {
        scores[i].nick = nick
        var s = scores[i].score
        s++
        scores[i].score = s
        server.announce(
            "<Trivia> " +
                nick +
                " is the winner of this round (" +
                questions[questionsCount].answer +
                ")! Your score is: " +
                s,
            false,
            null)
        if (ipStreak == ip) {
          scoreStreak++
          if (scoreStreak > 1) {
            try {
              Thread.sleep(20)
            } catch (e: Exception) {}
            server.announce("<Trivia> ***$nick has won $scoreStreak in a row!***", false, null)
          }
        } else {
          scoreStreak = 1
          ipStreak = ip
        }
        try {
          Thread.sleep(20)
        } catch (e: Exception) {}
        if (s == 25) {
          server.announce("<Trivia> $nick, you're doing great. Keep it up tiger!", false, null)
        } else if (s == 50) {
          server.announce(
              "<Trivia> $nick, you're so smart you're going to break the Trivia Bot!", false, null)
        } else if (s == 100) {
          server.announce(
              "<Trivia> $nick, you're in a league of your own. Nobody can compete!", false, null)
        } else if (s % 100 == 0) {
          server.announce("<Trivia> $nick, you're a God at SupraTrivia!", false, null)
        }
        return
      }
    }
    scores.add(Scores(nick, ip, 1))
    scoreStreak = 1
    ipStreak = ip
    server.announce(
        "<Trivia> $nick is the winner of this round ($answer)! His score is: 1", false, null)
  }

  fun displayHighScores(winner: Boolean) {
    var firstNick = ""
    var secondNick = ""
    var thirdNick = ""
    var firstScore = 0
    var secondScore = 0
    var thirdScore = 0
    var tempNick: String
    var tempScore = 0
    for (i in scores.indices) {
      if (scores[i].score > firstScore) {
        tempNick = firstNick
        tempScore = firstScore
        firstScore = scores[i].score
        firstNick = scores[i].nick
        secondScore = tempScore
        secondNick = tempNick
      } else if (scores[i].score > secondScore) {
        tempNick = secondNick
        tempScore = secondScore
        secondScore = scores[i].score
        secondNick = scores[i].nick
        thirdScore = tempScore
        thirdNick = tempNick
      } else if (scores[i].score > thirdScore) {
        thirdScore = scores[i].score
        thirdNick = scores[i].nick
      }
    }
    var str: String
    if (!winner) {
      str = "$firstNick = $firstScore, "
      str = "$str$secondNick = $secondScore, "
      str = "$str$thirdNick = $thirdScore"
      server.announce("<Trivia> " + "(Top 3 Scores of " + scores.size + ") " + str, false, null)
    } else {
      server.announce("<Trivia> The Winner is: $firstNick with $firstScore points!", false, null)
    }
  }

  fun saveScores(display: Boolean) {
    try {
      val out = Files.newBufferedWriter(Paths.get("scores.txt"), StandardCharsets.UTF_8)
      for (i in scores.indices) {
        out.write("ip:" + scores[i].iP)
        out.newLine()
        out.write("s:" + scores[i].score)
        out.newLine()
        out.write("n:" + scores[i].nick)
        out.newLine()
        out.newLine()
      }
      out.close()
      if (display)
          server.announce("<Trivia> " + "SupraTrivia Scores were Saved Successfully!", false, null)
    } catch (e: Exception) {
      server.announce("<Trivia> " + "Error Saving SupraTrivia Scores!", false, null)
    }
  }

  init {
    try {
      server.announce("<Trivia> " + "Loading SupraTrivia Questions...", false, null)
      var ist: InputStream = FileInputStream("questions.txt")
      var istream = BufferedReader(InputStreamReader(ist, StandardCharsets.UTF_8))
      var count = 0
      var str = istream.readLine() // First Question
      while (str != null) {
        if (str!!.startsWith("q:") || str!!.startsWith("Q:")) {
          var q: String = str!!.substring("q:".length).trim { it <= ' ' }
          str = istream.readLine()
          var a: String = str.substring("a:".length).trim { it <= ' ' }
          questions.add(Questions(q, a))
          questionsNum.add(count)
          count++
        }
        str = istream.readLine() // New Question
      }
      ist.close()
      server.announce("<Trivia> " + questions.size + " questions have been loaded!", false, null)

      // ##################
      // ######SCORES######
      // ##################
      server.announce("<Trivia> " + "Loading Previous Scores...", false, null)
      ist = FileInputStream("scores.txt")
      istream = BufferedReader(InputStreamReader(ist, StandardCharsets.UTF_8))
      str = istream.readLine() // First Score
      while (str != null) {
        if (str!!.startsWith("ip:") ||
            str!!.startsWith("IP:") ||
            str!!.startsWith("Ip:") ||
            str!!.startsWith("iP:")) {
          var ip: String = str!!.substring("ip:".length).trim { it <= ' ' }
          str = istream.readLine()
          var s: String = str.substring("s:".length).trim { it <= ' ' }
          str = istream.readLine()
          var n: String = str.substring("n:".length).trim { it <= ' ' }
          scores.add(Scores(n, ip, s.toInt()))
        }
        str = istream.readLine() // New Score
      }
      ist.close()
      server.announce("<Trivia> " + scores.size + " scores have been loaded!", false, null)
      if (questions.size == 0) {
        exitThread = true
      } else {
        server.switchTrivia = true
        server.announce("<Trivia> " + "SupraTrivia will begin in 10s!", false, null)
      }
    } catch (e: Exception) {
      exitThread = true
      server.announce("<Trivia> " + "Error loading SupraTrivia Questions/Scores!", false, null)
      // throw new RuntimeException("Error loading SupraTriva Questions! ", e);
    }
  }
}
