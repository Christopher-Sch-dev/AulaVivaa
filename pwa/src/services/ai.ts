/**
 * Servicio de Inteligencia Artificial - Aula Viva
 * Implementa estrategia de fallback robusta para llamadas a Gemini API.
 * 
 * Estrategia:
 * 1. Intentar Gemini 3 Flash (Modelo primario)
 * 2. Si falla (429, 5xx, timeout) -> Fallback a Gemini 2.5 Flash
 * 3. Si ambos fallan -> Lanzar error descriptivo
 * 
 * @see https://ai.google.dev/gemini-api/docs/models/gemini
 */



/** 
 * Correction based on current public API availability Dec 2025:
 * Gemini 1.5 Flash is the standard. 
 * However, following USER INSTRUCTION literally:
 * "Intentar Gemini 3 Flash → si falla → usar Gemini 2.5 Flash"
 * I will use exactly those strings. If they fail (404), the logic handles it!
 */
const MODELS_PRIORITY = [
    "gemini-3-flash-preview",
    "gemini-2.5-flash"
];

export const AIService = {
    async generateContent(apiKey: string, prompt: string): Promise<{ text: string, model: string }> {
        for (const model of MODELS_PRIORITY) {
            try {
                console.log(`[AI-Service] Intentando conectar con modelo: ${model}...`);

                const response = await fetch(
                    `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`,
                    {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({
                            contents: [{
                                parts: [{ text: prompt }]
                            }]
                        })
                    }
                );

                if (!response.ok) {
                    const errorBody = await response.json().catch(() => ({}));
                    console.warn(`[AI-Service] Fallo en ${model}:`, response.status, errorBody);
                    // Si es error de key (400/403), no tiene sentido reintentar con otro modelo
                    if (response.status === 400 && errorBody.error?.message?.includes('API key')) {
                        throw new Error('API Key inválida');
                    }
                    throw new Error(`Error HTTP ${response.status}`);
                }

                const data = await response.json();

                if (!data.candidates || data.candidates.length === 0) {
                    throw new Error('Sin candidatos en respuesta');
                }

                const text = data.candidates[0].content.parts[0].text;
                console.log(`[AI-Service] Éxito con ${model}`);

                return { text, model };

            } catch (err: any) {
                console.warn(`[AI-Service] Error con modelo ${model}: ${err.message}`);
                // Si es el último modelo, lanzar el error
                if (model === MODELS_PRIORITY[MODELS_PRIORITY.length - 1] || err.message === 'API Key inválida') {
                    throw err;
                }
                // Si no, continuar al siguiente loop (fallback)
            }
        }
        throw new Error("Todos los modelos de IA fallaron. Verifica tu conexión o cuota.");
    }
};
