package cl.duocuc.aulaviva.domain.repository

interface IIARepository {
    suspend fun generarIdeasParaClase(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun generarActividadesInteractivas(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun explicarConceptosParaAlumno(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun sugerirActividades(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun estructurarClasePorTiempo(
        nombreClase: String,
        descripcion: String,
        duracion: String,
        pdfUrl: String?
    ): String

    suspend fun resumirContenidoPdf(nombre: String, descripcion: String, pdfUrl: String?): String
    suspend fun generarGuiaPresentacion(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun generarEjerciciosParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun crearResumenEstudioParaAlumno(
        nombre: String,
        descripcion: String,
        pdfUrl: String?
    ): String

    suspend fun iniciarChatConContexto(
        nombreClase: String,
        descripcion: String,
        pdfUrl: String?,
        respuestaInicial: String
    )

    suspend fun enviarMensajeChat(mensaje: String): String
    suspend fun analizarPdfConIA(nombreClase: String, pdfUrl: String?): String

    // Nuevos métodos para persistencia y reanálisis
    suspend fun obtenerUltimaSesionParaClase(nombreClase: String): cl.duocuc.aulaviva.domain.model.ChatSession?
    suspend fun obtenerMensajesDeSesion(sessionId: Long): List<cl.duocuc.aulaviva.domain.model.ChatMessage>
    suspend fun reanalizarPdfParaSesion(sessionId: Long, pdfUrl: String?): String
    suspend fun cerrarSesion(sessionId: Long)
}
