require("dotenv").config();

const { onRequest } = require("firebase-functions/v2/https");
const logger = require("firebase-functions/logger");
const OpenAI = require("openai");

const client = new OpenAI({
  apiKey: process.env.OPENAI_API_KEY,
});

exports.analyzeBalance = onRequest(async (req, res) => {
  try {
    const text = req.body?.text;      // контекст пользователя
    const userData = req.body?.userData || "{}"; // сферы жизни, оценки и пр.

    if (!text) {
      return res.status(400).json({ error: "Текст не передан" });
    }

    if (!process.env.OPENAI_API_KEY) {
      return res.status(500).json({ error: "OpenAI API ключ отсутствует" });
    }

    // Системное сообщение — твой “промпт” коуча
    const systemMessage = `
Ты — поддерживающий и бережный коуч по балансу жизни.
Твоя задача — помочь человеку улучшить качество жизни без давления и тревоги.

Данные пользователя:
Сферы жизни и оценки по шкале от 1 до 10, которые берутся с колеса баланса жизни.

Контекст пользователя:
${text}

Сделай следующее:
1. Коротко и мягко проанализируй общее состояние.
2. Укажи все сферы, которые сейчас больше всего нуждаются во внимании, оценка которых меньше пяти.
3. Предложи три очень простых и реалистичных шага на сегодня.
4. Используй поддерживающий, спокойный тон.
5. Избегай резких формулировок, запугивания и давления.
6. Пиши короткими абзацами.

Ответ должен быть на языке пользователя.
`;

    const completionParams = {
      model: "gpt-4o-mini",
      messages: [
        { role: "system", content: systemMessage },
        { role: "user", content: text }
      ],
    };

    // Добавляем maxTokens только если он передан
    const maxTokens = req.body?.maxTokens;
    if (maxTokens != null && maxTokens > 0) {
      completionParams.max_tokens = maxTokens;
    }

    const completion = await client.chat.completions.create(completionParams);

    res.json({
      result: completion.choices[0].message.content,
    });
  } catch (err) {
    logger.error(err);
    res.status(500).json({ error: err.message });
  }
});
