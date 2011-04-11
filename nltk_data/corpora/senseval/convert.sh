for i in 1 1  1 1 1; do
sed -i -re "s/(<head>\w+<\/head>[^<]*)<head>(\w+)<\/head>/\1\2/g" $@ *
done
