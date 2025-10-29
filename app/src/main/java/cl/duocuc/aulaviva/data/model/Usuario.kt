package cl.duocuc.aulaviva.data.model

/**
 * Modelo de datos para representar un usuario de Aula Viva.
 * Puede ser Docente o Alumno.
 * 
 * Este modelo se guarda en Firestore en la colección "usuarios".
 * 
 * ROLES:
 * - "docente": Crea clases, sube materiales, gestiona contenido
 * - "alumno": Accede a clases, publica apuntes, responde quizzes
 * 
 * Pensamiento: Diferenciar roles permite dar permisos distintos
 * y personalizar la experiencia según el tipo de usuario.
 */
data class Usuario(
    val uid: String = "",           // ID único de Firebase Auth
    val email: String = "",         // Correo electrónico
    val rol: String = "alumno",     // "docente" o "alumno"
    val nombre: String = "",        // Nombre completo (opcional)
    val fechaRegistro: Long = System.currentTimeMillis()  // Timestamp de creación
)
