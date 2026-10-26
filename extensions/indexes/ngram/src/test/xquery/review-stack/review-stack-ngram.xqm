(:
 : eXist-db Open Source Native XML Database
 : Copyright (C) 2001 The eXist-db Authors
 :
 : info@exist-db.org
 : http://www.exist-db.org
 :
 : This library is free software; you can redistribute it and/or
 : modify it under the terms of the GNU Lesser General Public
 : License as published by the Free Software Foundation; either
 : version 2.1 of the License, or (at your option) any later version.
 :
 : This library is distributed in the hope that it will be useful,
 : but WITHOUT ANY WARRANTY; without even the implied warranty of
 : MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 : Lesser General Public License for more details.
 :
 : You should have received a copy of the GNU Lesser General Public
 : License along with this library; if not, write to the Free Software
 : Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 :)
xquery version "3.1";

(:~
 : Review of #6918: a wildcard query whose parts match on different nodes below the context node.
 :)
module namespace rsn = "http://exist-db.org/xquery/ngram/test/review-stack";

declare namespace test = "http://exist-db.org/xquery/xqsuite";

import module namespace ngram = "http://exist-db.org/xquery/ngram";

declare variable $rsn:NAME := "test-review-stack-ngram";
declare variable $rsn:COLLECTION := "/db/" || $rsn:NAME;
declare variable $rsn:CONFIG := "/db/system/config/db/" || $rsn:NAME;

declare variable $rsn:XCONF :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <ngram qname="el"/>
            <ngram qname="@att"/>
        </index>
    </collection>;

declare
    %test:setUp
function rsn:setUp() {
    xmldb:create-collection("/db/system", "config"),
    xmldb:create-collection("/db/system/config", "db"),
    xmldb:create-collection("/db/system/config/db", $rsn:NAME),
    xmldb:store($rsn:CONFIG, "collection.xconf", $rsn:XCONF),
    xmldb:create-collection("/db", $rsn:NAME),
    xmldb:store($rsn:COLLECTION, "doc.xml", <doc><el att="tx">xxxxee</el></doc>)
};

declare
    %test:tearDown
function rsn:tearDown() {
    xmldb:remove($rsn:COLLECTION),
    xmldb:remove($rsn:CONFIG)
};

(: "t" matches in the attribute, "ee" in the element's text: no match, as fn:matches says :)
declare
    %test:assertEquals(0, 0)
function rsn:wildcard-parts-on-different-nodes-variable() {
    let $els := collection($rsn:COLLECTION)//el
    return (
        count($els[matches(., "t.ee")]),
        count($els[ngram:wildcard-contains(., "t.ee")])
    )
};

declare
    %test:assertEquals(0)
function rsn:wildcard-parts-on-different-nodes-path() {
    count(collection($rsn:COLLECTION)//el[ngram:wildcard-contains(., "t.ee")])
};
