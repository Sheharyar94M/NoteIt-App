package playaxis.appinn.note_it.main.fragment_home.bottom_bar_fragments.draw.helper

data class SerializableDrawingPath(
    var color: Int,
    var brushThickness: Int,
    var alpha: Int,
    var pathCommands: List<PathCommand> = listOf()
)