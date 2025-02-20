#include "libs/AixLog/aixlog.hpp"

#define TOYVPN_LOG_INFO(message) LOG(INFO) << COLOR(green) << message << COLOR(none) << "\n";
#define TOYVPN_LOG_ERROR(message) LOG(ERROR) << COLOR(red) << message << COLOR(none) << "\n";
#define TOYVPN_LOG_DEBUG(message) LOG(DEBUG) << message << "\n";
