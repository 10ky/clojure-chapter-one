(defproject chapter-one "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/tools.logging "0.4.1"]
                 [org.apache.logging.log4j/log4j-core "2.11.0"]
                 [org.apache.logging.log4j/log4j-api "2.11.0"]
                 [org.apache.logging.log4j/log4j-slf4j-impl "2.11.0"]
                 [org.slf4j/jcl-over-slf4j "1.7.25"]
                 [clojure-term-colors "0.1.0"]
                 [org.clojure/spec.alpha "0.1.143"]
                 [org.clojure/test.check "0.10.0-alpha2"]
                 [provisdom/spectomic "0.7.6"]
                 [aero "1.1.3"]
                 [com.datomic/client-pro "0.8.14"]
                 [org.clojars.akiel/datomic-spec "0.2"]
                 ]
  :main ^:skip-aot chapter-one.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
