package com.wildrew.jobstat.statistics_read.develop

import com.wildrew.jobstat.statistics_read.core.core_mongo_base.model.BaseDocument
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.Field

@Document(collection = "user_dashboard_config")
class UserDashboardConfigDocument(
    id: String? = null,
    @Field("user_id")
    val userId: Long,
    @Field("preferred_metrics")
    val preferredMetrics: List<String>,
    @Field("preferred_entities")
    val preferredEntities: List<String>,
    @Field("dashboard_layout")
    val dashboardLayout: DashboardLayout,
    @Field("notification_settings")
    val notificationSettings: NotificationSettings,
) : BaseDocument(id) {
    override fun validate() {
        require(preferredMetrics.isNotEmpty()) { "Preferred metrics must not be empty" }
        require(preferredEntities.isNotEmpty()) { "Preferred entities must not be empty" }
    }

    data class DashboardLayout(
        @Field("widgets")
        val widgets: List<DashboardWidget>,
        @Field("layout_type")
        val layoutType: String,
    )

    data class DashboardWidget(
        @Field("widget_id")
        val widgetId: String,
        @Field("widget_type")
        val widgetType: String,
        @Field("position")
        val position: Position,
        @Field("settings")
        val settings: Map<String, Any>,
    )

    data class Position(
        @Field("row")
        val row: Int,
        @Field("column")
        val column: Int,
        @Field("size")
        val size: String,
    )

    data class NotificationSettings(
        @Field("email_notifications")
        val emailNotifications: Boolean,
        @Field("notification_preferences")
        val notificationPreferences: Map<String, Boolean>,
    )
}
