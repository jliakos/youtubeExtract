=====
Youtube link extractor + upload to a new playlist to youtube
=====
##Requirements

- Java 8

##Build

 	> gradle clean buildJar

 	

##USAGE
> java -jar youtube-1.0.jar -group facebookGroupName -url url to crawl -max max links to store default 100 --youtube -name playlistname"
> ie java -jar youtube-1.0.jar -group YOUcover --youtube

the --youtube flag uploads the youtube links found to youtube
