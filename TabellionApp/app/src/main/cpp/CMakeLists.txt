cmake_minimum_required(VERSION 3.4.1)

add_library(xmd-actions SHARED
            xmd-actions.c)

# Include libraries needed for xmd-actions lib
target_link_libraries(xmd-actions
                      android
                      log)
