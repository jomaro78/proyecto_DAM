# Esdeveniments 
Proyecto final del CFGS de Desarrollo de Aplicaciones Multiplataforma (DAM)

Aplicación Android desarrollada en Kotlin para la organización y consulta de eventos. Permite visualizar eventos por categoría, ubicarlos en el mapa, suscribirse, marcarlos como favoritos y participar en un chat en tiempo real asociado a cada evento.

## Funcionalidades principales

- Registro e inicio de sesión con Firebase Auth
- Vista de eventos por categoría
- Gestión de favoritos y eventos suscritos
- Ubicación geográfica de eventos
- Chat en tiempo real
- Arquitectura modular basada en MVVM

## Tecnologías utilizadas

- Kotlin
- Android Studio
- Firebase (Auth, Firestore, Storage)
- ViewModel, LiveData
- Jetpack Components
- Glide (para carga de imágenes)

## Estructura del código
app

└── src

     └── main
     
          ├── java/com/montilivi/esdeveniments
          
          │    ├── data
          
          │    ├── ui
          
          │    └── utils
          
          └── res

# Cómo ejecutar

1. Clonar el repositorio
2. Añadir tu archivo `google-services.json` en `app/`
3. Compilar desde Android Studio

> Nota: el archivo `google-services.json` no se incluye por motivos de seguridad

## Autor

Jordi Martín – Proyecto final de ciclo (2025)
