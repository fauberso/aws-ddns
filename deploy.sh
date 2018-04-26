#!/bin/bash
pushd . > /dev/null
cd $(dirname "$0")
mvn package
aws s3 cp target/awsddns-1.0.0.jar s3://net.auberson.awsddns
aws cloudformation deploy --stack-name ddns --template-file aws-route53-ddns.yaml --parameter-overrides "HostedZoneId=$ZONEID" "UserName=$USERNAME" "Password=$PASSWORD" --capabilities CAPABILITY_IAM
popd > /dev/null