package com.pcapplusplus.toyvpn.model

data class VpnSettings(
    val clientAddress: String,
    val clientAddressPrefixLength: Int,
    val routeAddress: String,
    val routePrefixLength: Int,
    val mtu: Int,
    val dnsServer: String? = null
) {
    companion object {
        fun fromParamString(parameters: String): VpnSettings {
            var clientAddress: String? = null
            var clientAddressPrefixLength: Int? = null
            var mtu: Int? = null
            var routeAddress: String? = null
            var routePrefixLength: Int? = null
            var dnsServer: String? = null

            for (parameter in parameters.split(" ")) {
                val fields = parameter.split(",")
                try {
                    when (fields[0].first()) {
                        'm' -> mtu = fields[1].toInt()
                        'a' -> {
                            clientAddress = fields[1]
                            clientAddressPrefixLength = fields[2].toInt()
                        }

                        'r' -> {
                            routeAddress = fields[1]
                            routePrefixLength = fields[2].toInt()
                        }

                        'd' -> dnsServer = fields[1]
                    }
                } catch (ex: Exception) {
                    when (ex) {
                        is NumberFormatException, is IndexOutOfBoundsException -> {
                            throw IllegalArgumentException("Bad parameter: $parameter")
                        }
                        else -> throw ex
                    }
                }
            }

            if (clientAddress == null || clientAddressPrefixLength == null) {
                throw IllegalArgumentException("clientAddress or clientAddressPrefixLength not provided")
            }

            return VpnSettings(
                clientAddress = clientAddress,
                clientAddressPrefixLength = clientAddressPrefixLength,
                dnsServer = dnsServer,
                mtu = mtu ?: 1400,
                routeAddress = routeAddress ?: "0.0.0.0",
                routePrefixLength = routePrefixLength ?: 0
            )
        }
    }
}
