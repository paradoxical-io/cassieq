#!/usr/bin/env bash

echo "Adding localhost multicast route..."
sudo route add -net 228.5.5.5/32 -interface lo0
