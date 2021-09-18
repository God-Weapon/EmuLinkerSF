package org.emulinker.kaillera.model.impl;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Trivia implements Runnable {
  private boolean exitThread = false;
  private boolean triviaPaused = false;
  private KailleraServerImpl server;

  private boolean newQuestion = true;
  private String answer;
  private char hint[];
  private boolean hint1 = false;
  private boolean hint2 = false;
  private boolean answered = false;
  private int questions_count = 0;
  private String ip_streak = "";
  private int score_streak = 0;
  private int questionTime = 30000;

  private List<Questions> questions = new ArrayList<Questions>();
  private List<Integer> questions_num = new ArrayList<Integer>();
  private List<Scores> scores = new ArrayList<Scores>();

  public boolean isAnswered() {
    return answered;
  }

  public void setQuestionTime(int questionTime) {
    this.questionTime = questionTime;
  }

  public List<Scores> getScores() {
    return scores;
  }

  public void setTriviaPaused(boolean triviaPaused) {
    this.triviaPaused = triviaPaused;
  }

  private class Scores {
    private String ip;
    private int score;
    private String nick;

    public Scores(String nick, String ip, int score) {
      this.nick = nick;
      this.ip = ip;
      this.score = score;
    }

    public void setScore(int score) {
      this.score = score;
    }

    public void setIP(String ip) {
      this.ip = ip;
    }

    public void setNick(String nick) {
      this.nick = nick;
    }

    public int getScore() {
      return score;
    }

    public String getIP() {
      return ip;
    }

    public String getNick() {
      return nick;
    }
  }

  private class Questions {
    String question;
    String answer;

    public Questions(String question, String answer) {
      this.question = question;
      this.answer = answer;
    }

    public void setQuestion(String question) {
      this.question = question;
    }

    public void setAnswer(String answer) {
      this.answer = answer;
    }

    public String getQuestion() {
      return question;
    }

    public String getAnswer() {
      return answer;
    }
  }

  public Trivia(KailleraServerImpl server) {
    this.server = server;

    try {
      server.announce("<Trivia> " + "Loading SupraTrivia Questions...", false, null);
      InputStream ist = new FileInputStream("questions.txt");
      BufferedReader istream = new BufferedReader(new InputStreamReader(ist, UTF_8));
      int count = 0;

      String str = istream.readLine(); // First Question
      while (str != null) {
        if (str.startsWith("q:") || str.startsWith("Q:")) {
          String q;
          String a;

          q = str.substring("q:".length()).trim();
          str = istream.readLine();
          a = str.substring("a:".length()).trim();

          questions.add(new Questions(q, a));
          questions_num.add(count);
          count++;
        }
        str = istream.readLine(); // New Question
      }
      ist.close();

      server.announce("<Trivia> " + questions.size() + " questions have been loaded!", false, null);

      // ##################
      // ######SCORES######
      // ##################
      server.announce("<Trivia> " + "Loading Previous Scores...", false, null);
      ist = new FileInputStream("scores.txt");
      istream = new BufferedReader(new InputStreamReader(ist, UTF_8));

      str = istream.readLine(); // First Score
      while (str != null) {
        if (str.startsWith("ip:")
            || str.startsWith("IP:")
            || str.startsWith("Ip:")
            || str.startsWith("iP:")) {
          String ip;
          String s;
          String n;

          ip = str.substring("ip:".length()).trim();
          str = istream.readLine();
          s = str.substring("s:".length()).trim();
          str = istream.readLine();
          n = str.substring("n:".length()).trim();

          scores.add(new Scores(n, ip, Integer.parseInt(s)));
        }
        str = istream.readLine(); // New Score
      }
      ist.close();

      server.announce("<Trivia> " + scores.size() + " scores have been loaded!", false, null);

      if (questions.size() == 0) {
        exitThread = true;
      } else {
        server.setSwitchTrivia(true);
        server.announce("<Trivia> " + "SupraTrivia will begin in 10s!", false, null);
      }
    } catch (Exception e) {
      exitThread = true;
      server.announce("<Trivia> " + "Error loading SupraTrivia Questions/Scores!", false, null);
      // throw new RuntimeException("Error loading SupraTriva Questions! ", e);
    }
  }

  @Override
  public void run() {
    int count = 0;
    int temp;
    Random generator = new Random();

    if (questions.size() > 1) {
      temp = generator.nextInt(questions_num.size() - 1);
      questions_count = questions_num.get(temp);
      questions_num.remove(temp);
    }
    try {
      Thread.sleep(10000);
    } catch (Exception e) {
    }

    while (!exitThread) {
      if (!triviaPaused) {
        if (newQuestion) {
          count++;
          if (count % 15 == 0) {
            saveScores(false);
            displayHighScores(false);
          }

          newQuestion = false;
          hint1 = true;
          hint2 = false;

          server.announce("<Trivia> " + questions.get(questions_count).getQuestion(), false, null);
          if (!answered)
            try {
              Thread.sleep(10000);
            } catch (Exception e) {
            }

          if (!answered) {
            server.announce("<Trivia> " + "35 seconds left...", false, null);
            try {
              Thread.sleep(5000);
            } catch (Exception e) {
            }
          }
        }
        if (hint1 && !answered) {
          newQuestion = false;
          hint1 = false;
          hint2 = true;

          answer = questions.get(questions_count).getAnswer().toLowerCase();
          answer = answer.replace(" ", "    ");
          hint = answer.toCharArray();
          for (int w = 0; w < hint.length; w++) {
            if ((w + 1) % 2 == 0 && hint[w] != ' ') {
              hint[w] = '_';
            }
          }

          answer = String.valueOf(hint);
          answer = answer.replace("_", " _ ");

          server.announce("<Trivia> " + "Hint1: " + answer, false, null);
          if (!answered)
            try {
              Thread.sleep(10000);
            } catch (Exception e) {
            }

          if (!answered) {
            server.announce("<Trivia> " + "20 seconds left...", false, null);
            try {
              Thread.sleep(5000);
            } catch (Exception e) {
            }
          }
        }

        if (hint2 && !answered) {
          newQuestion = false;
          hint1 = false;
          hint2 = false;

          answer = questions.get(questions_count).getAnswer().toLowerCase();
          answer = answer.replace(" ", "    ");
          hint = answer.toCharArray();

          for (int w = 0; w < hint.length; w++) {
            if ((w + 1) % 4 == 0 && hint[w] != ' ') {
              hint[w] = '_';
            }
          }

          answer = String.valueOf(hint);
          answer = answer.replace("_", " _ ");

          server.announce("<Trivia> " + "Hint2: " + answer, false, null);
          if (!answered)
            try {
              Thread.sleep(10000);
            } catch (Exception e) {
            }

          if (!answered) {
            server.announce("<Trivia> " + "5 seconds left...", false, null);
            try {
              Thread.sleep(5000);
            } catch (Exception e) {
            }
          }
        }

        if (!answered) {
          server.announce(
              "<Trivia> "
                  + "Time's up! The answer is: "
                  + questions.get(questions_count).getAnswer(),
              false,
              null);
        }

        if (count == questions.size()) {
          count = 0;
          server.announce(
              "<Trivia> " + "***All questions have been exhaused! Restarting list...***",
              false,
              null);
        }

        // Find questions not repeated
        if (questions_num.size() == 1) {
          questions_count = questions_num.get(0);
          questions_num.clear();
          for (int w = 0; w < questions.size(); w++) {
            questions_num.add(w);
          }
        } else {
          temp = generator.nextInt(questions_num.size() - 1);
          questions_count = questions_num.get(temp);
          questions_num.remove(temp);
        }
        try {
          Thread.sleep(5000);
        } catch (Exception e) {
        }

        server.announce(
            "<Trivia> "
                + (questionTime / 1000)
                + " seconds until the next question. Get ready for question "
                + (count + 1)
                + " of "
                + questions.size(),
            false,
            null);
        try {
          Thread.sleep(questionTime);
        } catch (Exception e) {
        }

        newQuestion = true;
        hint1 = false;
        hint2 = false;
        answered = false;
      } else {
        try {
          Thread.sleep(1000);
        } catch (Exception e) {
        }
      }
    }
  }

  public boolean isCorrect(String message) {
    String numbers0[] = {
      "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"
    };
    String numbers1[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
    String placement0[] = {
      "first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "nineth", "tenth"
    };
    String placement1[] = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"};

    if (message.toLowerCase().equals(questions.get(questions_count).getAnswer().toLowerCase())) {
      answered = true;
      return true;
    } else {
      for (int i = 0; i < numbers0.length; i++) {
        if (questions.get(questions_count).getAnswer().toLowerCase().contains(numbers0[i])) {
          message = message.replace(numbers1[i], numbers0[i]);
        } else if (questions.get(questions_count).getAnswer().toLowerCase().contains(numbers1[i])) {
          message = message.replace(numbers0[i], numbers1[i]);
        }
      }

      if (message.toLowerCase().equals(questions.get(questions_count).getAnswer().toLowerCase())) {
        answered = true;
        return true;
      } else {
        for (int i = 0; i < placement0.length; i++) {
          if (questions.get(questions_count).getAnswer().toLowerCase().contains(placement0[i])) {
            message = message.replace(placement1[i], placement0[i]);
          } else if (questions
              .get(questions_count)
              .getAnswer()
              .toLowerCase()
              .contains(placement1[i])) {
            message = message.replace(placement0[i], placement1[i]);
          }
        }

        if (message
            .toLowerCase()
            .equals(questions.get(questions_count).getAnswer().toLowerCase())) {
          answered = true;
          return true;
        }
      }
    }

    return false;
  }

  public boolean updateIP(String ip, String ip_update) {
    for (int i = 0; i < scores.size(); i++) {
      if (scores.get(i).getIP().equals(ip)) {
        scores.get(i).setIP(ip_update);
        return true;
      }
    }

    return false;
  }

  public void addScore(String nick, String ip, String answer) {
    answered = true;

    for (int i = 0; i < scores.size(); i++) {
      if (scores.get(i).getIP().equals(ip)) {
        scores.get(i).setNick(nick);
        int s = scores.get(i).getScore();
        s++;
        scores.get(i).setScore(s);
        server.announce(
            "<Trivia> "
                + nick
                + " is the winner of this round ("
                + questions.get(questions_count).getAnswer()
                + ")! Your score is: "
                + s,
            false,
            null);

        if (ip_streak.equals(ip)) {
          score_streak++;
          if (score_streak > 1) {
            try {
              Thread.sleep(20);
            } catch (Exception e) {
            }
            server.announce(
                "<Trivia> ***" + nick + " has won " + score_streak + " in a row!***", false, null);
          }
        } else {
          score_streak = 1;
          ip_streak = ip;
        }

        try {
          Thread.sleep(20);
        } catch (Exception e) {
        }
        if (s == 25) {
          server.announce(
              "<Trivia> " + nick + ", you're doing great. Keep it up tiger!", false, null);
        } else if (s == 50) {
          server.announce(
              "<Trivia> " + nick + ", you're so smart you're going to break the Trivia Bot!",
              false,
              null);
        } else if (s == 100) {
          server.announce(
              "<Trivia> " + nick + ", you're in a league of your own. Nobody can compete!",
              false,
              null);
        } else if (s % 100 == 0) {
          server.announce("<Trivia> " + nick + ", you're a God at SupraTrivia!", false, null);
        }
        return;
      }
    }

    scores.add(new Scores(nick, ip, 1));
    score_streak = 1;
    ip_streak = ip;
    server.announce(
        "<Trivia> " + nick + " is the winner of this round (" + answer + ")! His score is: 1",
        false,
        null);
  }

  public void displayHighScores(boolean winner) {
    String first_nick = "";
    String second_nick = "";
    String third_nick = "";
    int first_score = 0;
    int second_score = 0;
    int third_score = 0;
    String temp_nick;
    int temp_score = 0;

    for (int i = 0; i < scores.size(); i++) {
      if (scores.get(i).getScore() > first_score) {
        temp_nick = first_nick;
        temp_score = first_score;

        first_score = scores.get(i).getScore();
        first_nick = scores.get(i).getNick();

        second_score = temp_score;
        second_nick = temp_nick;
      } else if (scores.get(i).getScore() > second_score) {
        temp_nick = second_nick;
        temp_score = second_score;

        second_score = scores.get(i).getScore();
        second_nick = scores.get(i).getNick();

        third_score = temp_score;
        third_nick = temp_nick;
      } else if (scores.get(i).getScore() > third_score) {
        third_score = scores.get(i).getScore();
        third_nick = scores.get(i).getNick();
      }
    }

    String str;

    if (!winner) {
      str = first_nick + " = " + first_score + ", ";
      str = str + second_nick + " = " + second_score + ", ";
      str = str + third_nick + " = " + third_score;
      server.announce("<Trivia> " + "(Top 3 Scores of " + scores.size() + ") " + str, false, null);
    } else {
      server.announce(
          "<Trivia> " + "The Winner is: " + first_nick + " with " + first_score + " points!",
          false,
          null);
    }
  }

  public void saveScores(boolean display) {
    try {
      BufferedWriter out = Files.newBufferedWriter(Paths.get("scores.txt"), UTF_8);

      for (int i = 0; i < scores.size(); i++) {
        out.write("ip:" + scores.get(i).getIP());
        out.newLine();
        out.write("s:" + scores.get(i).getScore());
        out.newLine();
        out.write("n:" + scores.get(i).getNick());
        out.newLine();
        out.newLine();
      }
      out.close();

      if (display)
        server.announce("<Trivia> " + "SupraTrivia Scores were Saved Successfully!", false, null);
    } catch (Exception e) {
      server.announce("<Trivia> " + "Error Saving SupraTrivia Scores!", false, null);
    }
  }
}
