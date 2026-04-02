import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

internal fun JsonObjectBuilder.collectionIdSchema() {
    put("collectionId", buildJsonObject {
        put("type", "number")
        put(
            "description",
            "Optional ID of the targeted calendar collection. Must be empty (= default calendar) or a collection ID as returned by collections.list."
        )
    })
}