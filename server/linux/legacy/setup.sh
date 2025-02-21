sh -c "echo 1 > /proc/sys/net/ipv4/ip_forward"
iptables -t nat -A POSTROUTING -s 10.0.0.0/8 -o eth0 -j MASQUERADE
ip tuntap add dev tun0 mode tun
ifconfig tun0 10.0.0.1 dstaddr 10.0.0.2 up
