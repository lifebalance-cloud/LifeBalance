package com.example.mylife.lifebalance.utils
import com.example.mylife.lifebalance.data.LifeSphere
import com.example.mylife.lifebalance.data.Task
object TaskGenerator {
    private val taskTemplates = mapOf(
        "Здоровье" to mapOf(
            0..4 to listOf(
                "Начните с 10-минутной зарядки по утрам",
                "Установите режим сна: ложитесь и вставайте в одно время",
                "Пейте 2 литра воды в день"
            ),
            5..7 to listOf(
                "Добавьте в рацион больше овощей и фруктов",
                "Запишитесь на плановый медосмотр"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Семья" to mapOf(
            0..4 to listOf(
                "Организуйте семейный ужин без гаджетов",
                "Проведите выходные с семьей",
                "Поговорите с каждым членом семьи по душам"
            ),
            5..7 to listOf(
                "Запланируйте совместное мероприятие на выходные",
                "Помогите с домашними делами"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Работа" to mapOf(
            0..4 to listOf(
                "Составьте план задач на неделю",
                "Пройдите курс повышения квалификации",
                "Обсудите с руководителем перспективы развития"
            ),
            5..7 to listOf(
                "Улучшите рабочий процесс",
                "Найдите баланс между работой и отдыхом"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Друзья" to mapOf(
            0..4 to listOf(
                "Напишите старым друзьям",
                "Организуйте встречу с друзьями",
                "Присоединитесь к новому сообществу по интересам"
            ),
            5..7 to listOf(
                "Предложите друзьям совместное мероприятие",
                "Поддерживайте регулярное общение"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Финансы" to mapOf(
            0..4 to listOf(
                "Составьте бюджет на месяц",
                "Откройте накопительный счет",
                "Изучите основы финансовой грамотности"
            ),
            5..7 to listOf(
                "Проанализируйте расходы и найдите способы сэкономить",
                "Рассмотрите дополнительные источники дохода"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Отдых" to mapOf(
            0..4 to listOf(
                "Запланируйте выходной полностью для отдыха",
                "Найдите новое хобби",
                "Проведите день без работы и обязанностей"
            ),
            5..7 to listOf(
                "Запланируйте небольшое путешествие",
                "Выделите время для любимого занятия"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Духовность" to mapOf(
            0..4 to listOf(
                "Выделите 10 минут на медитацию или молитву",
                "Читайте вдохновляющие книги",
                "Проведите время на природе"
            ),
            5..7 to listOf(
                "Практикуйте благодарность",
                "Развивайте духовные практики"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        ),
        "Саморазвитие" to mapOf(
            0..4 to listOf(
                "Запишитесь на онлайн-курс",
                "Читайте 20 минут в день",
                "Начните вести дневник"
            ),
            5..7 to listOf(
                "Установите конкретную цель для развития",
                "Изучите что-то новое"
            ),
            8..10 to listOf("Отлично! Продолжайте в том же духе")
        )
    )
    fun generateTasks(sphere: LifeSphere): List<Task> {
        val tasks = mutableListOf<Task>()
        val sphereName = sphere.name
        val score = sphere.score

        val templates = taskTemplates[sphereName] ?: getDefaultTemplates(score)

        val taskCount = when {
            score <= 4 -> 3
            score in 5..7 -> 2
            else -> 1
        }
        templates.keys.forEach { range ->
            if (score in range) {
                val selectedTasks = templates[range]?.take(taskCount) ?: emptyList()
                selectedTasks.forEachIndexed { index, title ->
                    tasks.add(
                        Task(
                            sphereId = sphere.id,
                            title = title,
                            description = "Рекомендация для улучшения сферы \"$sphereName\""
                        )
                    )
                }
            }
        }

        return tasks
    }
    private fun getDefaultTemplates(score: Int): Map<IntRange, List<String>> {
        return when {
            score <= 4 -> mapOf(
                0..4 to listOf(
                    "Проанализируйте текущую ситуацию",
                    "Составьте план действий",
                    "Начните с малых шагов"
                )
            )
            score in 5..7 -> mapOf(
                5..7 to listOf(
                    "Продолжайте развивать эту сферу",
                    "Ставьте новые цели"
                )
            )
            else -> mapOf(
                8..10 to listOf("Отлично! Продолжайте в том же духе")
            )
        }
    }
}
