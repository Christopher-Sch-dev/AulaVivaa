package cl.duocuc.aulaviva.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import cl.duocuc.aulaviva.R

/**
 * Helper para manejar notificaciones push en la app.
 * Este es uno de los RECURSOS NATIVOS de Android que uso.
 * 
 * Las notificaciones mejoran la experiencia porque:
 * - Avisan al usuario de eventos importantes
 * - Funcionan incluso si la app está cerrada
 * - Son parte del sistema operativo Android
 * 
 * Pensamiento: Las notificaciones son clave en apps educativas.
 * El profe puede avisar de una clase nueva, o el alumno recibe recordatorios.
 */
class NotificationHelper(private val context: Context) {
    
    private val CHANNEL_ID = "aulaviva_channel"
    private val CHANNEL_NAME = "Notificaciones Aula Viva"
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    init {
        crearCanalNotificaciones()
    }
    
    /**
     * Crea el canal de notificaciones (obligatorio desde Android 8.0)
     * Los canales permiten que el usuario controle qué notificaciones recibe.
     */
    private fun crearCanalNotificaciones() {
        // Solo necesario en Android 8.0 (Oreo) o superior
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH  // Prioridad alta (hace sonido)
            ).apply {
                description = "Notificaciones de clases, tareas y recordatorios"
                enableLights(true)  // LED de notificación
                enableVibration(true)  // Vibración
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Envía una notificación simple.
     * @param titulo: Título de la notificación
     * @param mensaje: Contenido del mensaje
     * @param icono: ID del icono (opcional)
     */
    fun enviarNotificacion(
        titulo: String, 
        mensaje: String,
        icono: Int = R.drawable.ic_launcher_foreground
    ) {
        // Construyo la notificación con NotificationCompat (compatible con todas las versiones)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icono)  // Icono pequeño (obligatorio)
            .setContentTitle(titulo)  // Título grande
            .setContentText(mensaje)  // Texto del mensaje
            .setPriority(NotificationCompat.PRIORITY_HIGH)  // Prioridad alta
            .setAutoCancel(true)  // Se cierra al tocarla
            .setStyle(  // Estilo expandido para mensajes largos
                NotificationCompat.BigTextStyle()
                    .bigText(mensaje)
            )
        
        // Genero un ID único basado en el tiempo para que no se reemplacen
        val notificationId = System.currentTimeMillis().toInt()
        
        // Muestro la notificación
        notificationManager.notify(notificationId, builder.build())
    }
    
    /**
     * Envía una notificación de bienvenida personalizada
     */
    fun enviarNotificacionBienvenida(nombreUsuario: String) {
        enviarNotificacion(
            titulo = "¡Bienvenido a Aula Viva! 👋",
            mensaje = "Hola $nombreUsuario, estás listo para gestionar tus clases"
        )
    }
    
    /**
     * Notificación cuando se crea una clase nueva
     */
    fun notificarClaseCreada(nombreClase: String) {
        enviarNotificacion(
            titulo = "Clase creada exitosamente 📚",
            mensaje = "\"$nombreClase\" está lista para tus estudiantes"
        )
    }
    
    /**
     * Notificación de recordatorio
     */
    fun notificarRecordatorio(mensaje: String) {
        enviarNotificacion(
            titulo = "Recordatorio 🔔",
            mensaje = mensaje
        )
    }
}
