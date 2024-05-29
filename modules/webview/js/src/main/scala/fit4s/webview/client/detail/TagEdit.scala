package fit4s.webview.client.detail

import cats.Eq
import cats.effect.*
import cats.syntax.all.*
import fs2.concurrent.SignallingRef

import fit4s.activities.data.{ActivityId, Tag, TagName}
import fit4s.webview.client.FetchResult
import fit4s.webview.client.cmd.*
import fit4s.webview.client.shared._
import fit4s.webview.data.RequestFailure

import calico.html.io.{*, given}

object TagEdit:

  final case class Model(
      id: ActivityId,
      active: Set[Tag],
      menuOpen: Boolean,
      query: TextField.Model,
      results: List[Tag],
      error: Option[RequestFailure]
  ):
    def tags: Model.TagSelection = Model.TagSelection(active, results)

  object Model:
    def apply(id: ActivityId, active: Vector[Tag]): Model =
      Model(id, active.toSet, false, TextField.Model.empty, Nil, None)

    val menuStyle =
      "absolute left-0 top-8 max-h-64 w-full overflow-y-auto z-1500 border shadow-lg transition duration-200 bg-slate-50 dark:bg-stone-800 dark:border-stone-800 dark:text-stone-200"
    val buttonStyle =
      "text-xs h-8 w-8 hover:border dark:border-stone-400 hover:bg-slate-100 hover:dark:bg-stone-700  flex flex-row items-center px-2 py-0.5 rounded cursor-pointer"
    val disabledButton =
      "text-xs h-8 w-8 dark:border-stone-400 flex flex-row items-center px-2 py-0.5 rounded opacity-50 cursor-not-allowed"

    final case class TagSelection(
        active: Set[Tag],
        results: List[Tag]
    ):
      val all = (active ++ results).toList.distinct.sortBy(_.name.name)
      def isActive(t: Tag) = active.contains(t)
      def notActive(t: Tag) = !isActive(t)
    object TagSelection:
      given Eq[TagSelection] = Eq.fromUniversalEquals

  enum Msg:
    case ToggleMenu
    case CloseMenu
    case SearchTags
    case SearchTagsResult(r: FetchResult[List[Tag]])
    case ToggleTag(tag: Tag)
    case CreateTag
    case TagsRemoved(tag: Tag)
    case TagsAdded(tag: Tag)

  def update(cr: CommandRuntime[IO], msg: Msg)(model: Model): (Model, IO[Unit]) =
    msg match
      case Msg.ToggleMenu =>
        val cmd =
          if (model.menuOpen || model.results.nonEmpty) IO.unit
          else cr.send(Cmd.GetTags(model.query.text))
        (model.copy(menuOpen = !model.menuOpen), cmd)

      case Msg.CloseMenu =>
        (model.copy(menuOpen = false), IO.unit)

      case Msg.SearchTags =>
        (model, cr.send(Cmd.GetTags(model.query.text)))

      case Msg.SearchTagsResult(r) =>
        val next =
          r.fold(
            tags => model.copy(results = tags, error = None),
            err => model.copy(error = err.some)
          )
        (next, IO.unit)

      case Msg.ToggleTag(tag) =>
        val cmd =
          if (model.tags.isActive(tag)) Cmd.RemoveTag(model.id, tag)
          else Cmd.SetTag(model.id, tag)
        (model, cr.send(cmd))

      case Msg.CreateTag =>
        TagName.fromString(model.query.text) match
          case Right(tn) =>
            val cmd = Cmd.CreateTag(model.id, tn)
            (model, cr.send(cmd))
          case Left(_) =>
            (model, IO.unit)

      case Msg.TagsRemoved(tag) =>
        (model.copy(active = model.active - tag), cr.send(Cmd.SearchRefresh))

      case Msg.TagsAdded(tag) =>
        (model.copy(active = model.active + tag), cr.send(Cmd.SearchRefresh))

  def subscribe(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    cr.subscribe.evalMap:
      case Result.GetTagsResult(r) =>
        model.flatModify(update(cr, Msg.SearchTagsResult(r)))

      case Result.SetTagsResult(id, tags, true) =>
        model.get.flatMap:
          case m if m.id == id =>
            model.flatModify(update(cr, Msg.TagsAdded(tags)))
          case _ => IO.unit

      case Result.RemoveTagsResult(id, tag, true) =>
        model.get.flatMap:
          case m if m.id == id =>
            model.flatModify(update(cr, Msg.TagsRemoved(tag)))
          case _ => IO.unit

      case Result.CreateTagResult(id, name, true) =>
        model.get.flatMap:
          case m if m.id == id =>
            model.flatModify(update(cr, Msg.TagsAdded(Tag.create(name))))
          case _ => IO.unit

      case _ =>
        IO.unit

  def render(model: SignallingRef[IO, Model], cr: CommandRuntime[IO]) =
    Resource
      .eval(subscribe(model, cr).compile.drain.start)
      .as(
        div(
          cls := "flex flex-row items-start relative",
          div(
            cls := "flex flex-grow",
            model.map(_.active.toList).changes.map(TagList(_, None))
          ),
          div(
            cls := "flex",
            calico.html.io.a(
              cls := Model.buttonStyle,
              title := "Add/remove tags",
              i(cls := "fa fa-tag"),
              onClick --> ClickAction(model.flatModify(update(cr, Msg.ToggleMenu)))
            ),
            div(
              cls <-- model
                .map(_.menuOpen)
                .changes
                .map(b => List(if (b) Model.menuStyle else "hidden")),
              div(
                cls := "flex flex-row text-sm",
                TextField.render(
                  SignallingRef.lens(model)(_.query, a => b => a.copy(query = b)),
                  TextField.Config.default.copy(
                    cls =
                      "px-1 py-0.5 flex-grow border dark:border-0 dark:bg-stone-800 rounded",
                    onEnter = model.flatModify(update(cr, Msg.SearchTags))
                  )
                ),
                calico.html.io.a(
                  cls <-- model.map(m => TagName.fromString(m.query.text)).changes.map {
                    case Left(_)  => Model.disabledButton :: Nil
                    case Right(_) => Model.buttonStyle :: Nil
                  },
                  i(cls := "fa fa-plus"),
                  title := "Create a new tag",
                  onClick --> ClickAction(model.flatModify(update(cr, Msg.CreateTag)))
                ),
                calico.html.io.a(
                  cls := Model.buttonStyle,
                  i(cls := "fa fa-search"),
                  onClick --> ClickAction(model.flatModify(update(cr, Msg.SearchTags)))
                )
              ),
              model.map(_.tags).changes.map { tags =>
                tags.all.traverse(tagMenuRow(model, cr, tags)).flatMap(c => div(c))
              }
            )
          )
        )
      )

  private def tagMenuRow(
      model: SignallingRef[IO, Model],
      cr: CommandRuntime[IO],
      tags: Model.TagSelection
  )(tag: Tag) =
    calico.html.io.a(
      cls := Styles(
        "py-2 flex flex-row items-center cursor-pointer hover:bg-slate-100 dark:hover:bg-stone-700" -> true,
        "font-bold" -> tags.isActive(tag)
      ),
      onClick --> ClickAction(model.flatModify(update(cr, Msg.ToggleTag(tag)))),
      div(
        cls := "px-1 mr-2",
        i(
          cls := Styles(
            "fa fa-check" -> tags.isActive(tag),
            "fa-regular fa-square" -> tags.notActive(tag)
          )
        )
      ),
      div(
        cls := "flex flex-grow",
        tag.name.name
      )
    )
