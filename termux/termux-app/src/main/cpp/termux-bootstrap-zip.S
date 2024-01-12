     .global blob
     .global blob_size
     .section .rodata
 blob:
#if defined __aarch64__
     .incbin "/data/user/0/com.itsaky.androidide/files/home/projects/AndroidIDE/termux/termux-app/build/intermediates/bootstrap-packages/bootstrap-aarch64.zip"
 #elif defined __arm__
     .incbin "/data/user/0/com.itsaky.androidide/files/home/projects/AndroidIDE/termux/termux-app/build/intermediates/bootstrap-packages/bootstrap-arm.zip"
 #elif defined __x86_64__
     .incbin "/data/user/0/com.itsaky.androidide/files/home/projects/AndroidIDE/termux/termux-app/build/intermediates/bootstrap-packages/bootstrap-x86_64.zip"
 #else
 # error Unsupported arch
 #endif
 1:
 blob_size:
     .int 1b - blob
 