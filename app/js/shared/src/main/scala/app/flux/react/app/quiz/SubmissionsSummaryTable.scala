package app.flux.react.app.quiz

import scala.collection.immutable.Seq
import app.common.AnswerBullet
import app.flux.stores.quiz.GamepadStore.GamepadState
import app.flux.stores.quiz.SubmissionsSummaryStore
import app.flux.stores.quiz.TeamInputStore
import app.flux.stores.quiz.TeamsAndQuizStateStore
import app.models.quiz.config.QuizConfig
import app.models.quiz.QuizState
import app.models.quiz.QuizState.Submission
import app.models.quiz.QuizState.Submission.SubmissionValue
import app.models.quiz.Team
import app.models.quiz.config.QuizConfig.Question
import app.models.quiz.config.QuizConfig.Round
import hydro.common.I18n
import hydro.common.JsLoggingUtils.logExceptions
import hydro.common.JsLoggingUtils.LogExceptionsCallback
import hydro.flux.action.Dispatcher
import hydro.flux.react.HydroReactComponent
import hydro.flux.react.ReactVdomUtils.<<
import hydro.flux.react.ReactVdomUtils.^^
import hydro.flux.react.uielements.Bootstrap
import hydro.flux.react.uielements.PageHeader
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.vdom.html_<^.<

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js

final class SubmissionsSummaryTable(
    implicit quizConfig: QuizConfig,
    teamsAndQuizStateStore: TeamsAndQuizStateStore,
    submissionsSummaryStore: SubmissionsSummaryStore,
    i18n: I18n,
) extends HydroReactComponent {

  // **************** API ****************//
  def apply(selectedTeamId: Option[Long]): VdomElement = {
    component(Props(selectedTeamId = selectedTeamId))
  }

  // **************** Implementation of HydroReactComponent methods ****************//
  override protected val config =
    ComponentConfig(backendConstructor = new Backend(_), initialState = State())
      .withStateStoresDependency(
        teamsAndQuizStateStore,
        _.copy(teams = teamsAndQuizStateStore.stateOrEmpty.teams))
      .withStateStoresDependency(
        submissionsSummaryStore,
        _.copy(submissionsSummaryState = submissionsSummaryStore.stateOrEmpty))

  // **************** Implementation of HydroReactComponent types ****************//
  protected case class Props(selectedTeamId: Option[Long])
  protected case class State(
      teams: Seq[Team] = Seq(),
      submissionsSummaryState: SubmissionsSummaryStore.State = SubmissionsSummaryStore.State.nullInstance,
  )

  protected class Backend($ : BackendScope[Props, State]) extends BackendBase($) {

    override def render(props: Props, state: State): VdomNode = logExceptions {
      implicit val _ = props
      implicit val __ = state
      <.div(
        ^.className := "table-responsive",
        <.table(
          ^.className := "table table-bordered table-hover table-condensed",
          <.tr(
            <.th(i18n("app.question")), {
              for (team <- state.teams)
                yield
                  <.th(
                    ^.key := team.id,
                    ^^.ifThen(props.selectedTeamId == Some(team.id)) {
                      ^.className := "info"
                    },
                    team.name,
                  )
            }.toVdomArray
          ), {
            for ((round, roundIndex) <- quizConfig.rounds.zipWithIndex) yield {
              roundTitleRow(round) +: round.questions.zipWithIndex.map {
                case (question, questionIndex) => questionRow(question, roundIndex, questionIndex)
              }
            }
          }.flatten.toVdomArray
        ),
      )
    }

    private def roundTitleRow(round: Round): VdomNode = {
      <.tr(
        <.th(
          ^.colSpan := 999,
          round.name,
        ),
      )
    }

    private def questionRow(question: Question, roundIndex: Int, questionIndex: Int)(
        implicit state: State,
        props: Props,
    ): VdomNode = {
      <.tr(
        <.td(
          question match {
            case question: Question.Single => s"${question.question} (${question.answer})"
            case question: Question.Double => s"${question.textualQuestion} (${question.textualAnswer})"
          },
        ), {
          for (team <- state.teams)
            yield
              <.td(
                ^.key := team.id,
                ^^.ifThen(props.selectedTeamId == Some(team.id)) {
                  ^.className := "info"
                },
                <<.ifThen(state.submissionsSummaryState.hasAnySubmission(roundIndex, questionIndex, team.id)) {
                  state.submissionsSummaryState.points(roundIndex, questionIndex, team.id)
                },
              )
        }.toVdomArray
      )
    }
  }
}
