package com.tk.spycan

class CameraUnavailableException(val lens: Lens, cause: Throwable) :
    RuntimeException("[CameraXRecorder] camera unavailable for lens: $lens", cause)
