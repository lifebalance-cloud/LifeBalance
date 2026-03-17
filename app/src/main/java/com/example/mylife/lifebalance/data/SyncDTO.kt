package com.example.mylife.lifebalance.data

import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime

// DTO классы для синхронизации с Firestore
// Firestore не поддерживает напрямую LocalDate, LocalTime и enum, поэтому используем строки

data class LifeSphereDTO(
    var id: Long = 0,
    var name: String = "",
    var score: Int = 0,
    var colorIndex: Int = 0,
    var order: Int = 0
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, "", 0, 0, 0)
    fun toLifeSphere(): LifeSphere {
        return LifeSphere(
            id = id,
            name = name,
            score = score,
            colorIndex = colorIndex,
            order = order
        )
    }
    
    companion object {
        fun fromLifeSphere(sphere: LifeSphere): LifeSphereDTO {
            return LifeSphereDTO(
                id = sphere.id,
                name = sphere.name,
                score = sphere.score,
                colorIndex = sphere.colorIndex,
                order = sphere.order
            )
        }
    }
}

data class TaskDTO(
    var id: Long = 0,
    var sphereId: Long = 0,
    var title: String = "",
    var description: String = "",
    var isCompleted: Boolean = false,
    var date: String = LocalDate.now().toString(), // LocalDate как строка
    var time: String? = null, // LocalTime как строка
    var hasNotification: Boolean = false,
    var notificationSound: String = "default",
    var autoReschedule: Boolean = false,
    var repeatType: String = "NONE", // RepeatType как строка
    var repeatEndDate: String? = null // LocalDate как строка
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, 0, "", "", false, LocalDate.now().toString(), null, false, "default", false, "NONE", null)
    fun toTask(): Task {
        return Task(
            id = id,
            sphereId = sphereId,
            title = title,
            description = description,
            isCompleted = isCompleted,
            date = LocalDate.parse(date),
            time = time?.let { LocalTime.parse(it) },
            hasNotification = hasNotification,
            notificationSound = notificationSound,
            autoReschedule = autoReschedule,
            repeatType = RepeatType.valueOf(repeatType),
            repeatEndDate = repeatEndDate?.let { LocalDate.parse(it) }
        )
    }
    
    companion object {
        fun fromTask(task: Task): TaskDTO {
            return TaskDTO(
                id = task.id,
                sphereId = task.sphereId,
                title = task.title,
                description = task.description,
                isCompleted = task.isCompleted,
                date = task.date.toString(),
                time = task.time?.toString(),
                hasNotification = task.hasNotification,
                notificationSound = task.notificationSound,
                autoReschedule = task.autoReschedule,
                repeatType = task.repeatType.name,
                repeatEndDate = task.repeatEndDate?.toString()
            )
        }
    }
}

data class GoalDTO(
    var id: Int = 0,
    var sphereId: Int = 0,
    var text: String = "",
    var deadline: String = LocalDate.now().toString(), // Store as String
    var photoUris: String? = null,
    var link: String? = null,
    var checked: Boolean = false
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, 0, "", LocalDate.now().toString(), null, null, false)
    fun toGoal(): Goal {
        return Goal(
            id = id,
            sphereId = sphereId,
            text = text,
            deadline = LocalDate.parse(deadline),
            photoUris = photoUris,
            link = link,
            checked = checked
        )
    }

    companion object {
        fun fromGoal(goal: Goal): GoalDTO {
            return GoalDTO(
                id = goal.id,
                sphereId = goal.sphereId,
                text = goal.text,
                deadline = goal.deadline.toString(),
                photoUris = goal.photoUris,
                link = goal.link,
                checked = goal.checked
            )
        }
    }
}

data class IdeaFolderDTO(
    var id: Long = 0,
    var name: String = "",
    var createdAt: Long = System.currentTimeMillis()
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, "", System.currentTimeMillis())
    fun toIdeaFolder(): IdeaFolder {
        return IdeaFolder(id, name, createdAt)
    }

    companion object {
        fun fromIdeaFolder(folder: IdeaFolder): IdeaFolderDTO {
            return IdeaFolderDTO(folder.id, folder.name, folder.createdAt)
        }
    }
}

data class IdeaNoteDTO(
    var id: Long = 0,
    var folderId: Long? = null,
    var text: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, null, "", System.currentTimeMillis(), System.currentTimeMillis())
    fun toIdeaNote(): IdeaNote {
        return IdeaNote(id, folderId, text, createdAt, updatedAt)
    }

    companion object {
        fun fromIdeaNote(note: IdeaNote): IdeaNoteDTO {
            return IdeaNoteDTO(note.id, note.folderId, note.text, note.createdAt, note.updatedAt)
        }
    }
}

data class DreamAffirmationDTO(
    var id: Long = 0,
    var sectorId: Int = 0,
    var text: String = "",
    var createdAt: Long = System.currentTimeMillis(),
    var updatedAt: Long = System.currentTimeMillis()
) {
    // Конструктор без параметров для Firestore
    constructor() : this(0, 0, "", System.currentTimeMillis(), System.currentTimeMillis())
    
    fun toDreamAffirmation(): DreamAffirmation {
        return DreamAffirmation(
            id = id,
            sectorId = sectorId,
            text = text,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    companion object {
        fun fromDreamAffirmation(affirmation: DreamAffirmation): DreamAffirmationDTO {
            return DreamAffirmationDTO(
                id = affirmation.id,
                sectorId = affirmation.sectorId,
                text = affirmation.text,
                createdAt = affirmation.createdAt,
                updatedAt = affirmation.updatedAt
            )
        }
    }
}
