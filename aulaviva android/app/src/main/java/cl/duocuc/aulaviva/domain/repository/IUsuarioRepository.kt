package cl.duocuc.aulaviva.domain.repository

interface IUsuarioRepository {
    suspend fun guardarUsuario(uid: String, email: String, rol: String): Result<Unit>
    suspend fun obtenerUsuario(uid: String): Result<cl.duocuc.aulaviva.data.repository.UsuarioDTO>
    suspend fun actualizarRol(uid: String, nuevoRol: String): Result<Unit>
}
