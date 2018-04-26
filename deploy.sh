#!/bin/bash
pushd . > /dev/null
cd $(dirname "$0")
mvn package
aws cloudformation package --template-file route53-ddns.sam-template.yaml --s3-bucket net.auberson.awsddns --output-template-file route53-ddns.yaml
aws cloudformation deploy --stack-name ddns --template-file route53-ddns.yaml --parameter-overrides "HostedZoneId=$ZONEID" "UserName=$USERNAME" "Password=$PASSWORD" --capabilities CAPABILITY_IAM
popd > /dev/null