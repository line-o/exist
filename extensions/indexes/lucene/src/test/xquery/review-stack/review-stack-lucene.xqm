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
 : Review of the Lucene PRs of stack #6920 (#6915, #6916, #6917, #6919, #6926): each test states what the query
 : should give and fails on the stack.
 :)
module namespace rsl = "http://exist-db.org/xquery/lucene/test/review-stack";

declare namespace test = "http://exist-db.org/xquery/xqsuite";
declare namespace exist = "http://exist.sourceforge.net/NS/exist";

import module namespace ft = "http://exist-db.org/xquery/lucene";

declare variable $rsl:NAME := "test-review-stack-lucene";
declare variable $rsl:COLLECTION := "/db/" || $rsl:NAME;
declare variable $rsl:CONFIG := "/db/system/config/db/" || $rsl:NAME;

declare %private function rsl:store($sub as xs:string, $xconf as element(), $doc as element()) {
    let $config := xmldb:create-collection($rsl:CONFIG, $sub)
    let $col := xmldb:create-collection($rsl:COLLECTION, $sub)
    let $_ := xmldb:store($config, "collection.xconf", $xconf)
    return xmldb:store($col, "doc.xml", $doc)
};

declare %private function rsl:facet-xconf($hierarchical as xs:boolean) as element() {
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <text qname="rsl-p">
                    <facet dimension="rsl-kind" expression="'para'">{
                        if ($hierarchical) then attribute hierarchical { "yes" } else ()
                    }</facet>
                </text>
            </lucene>
        </index>
    </collection>
};

(: the same configuration, stored once per collection :)
declare variable $rsl:SCOPE_XCONF :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <analyzer class="org.apache.lucene.analysis.standard.StandardAnalyzer"/>
                <text qname="rsl-para">
                    <field name="rsl-content" expression="."/>
                    <facet dimension="rsl-scope-kind" expression="'para'"/>
                </text>
            </lucene>
        </index>
    </collection>;

declare variable $rsl:STOP_XCONF :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <analyzer class="org.apache.lucene.analysis.en.EnglishAnalyzer"/>
                <text qname="rsl-line"/>
            </lucene>
        </index>
    </collection>;

declare variable $rsl:NESTED_XCONF :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <analyzer class="org.apache.lucene.analysis.standard.StandardAnalyzer"/>
                <text qname="div"/>
                <text qname="p"/>
            </lucene>
        </index>
    </collection>;

declare variable $rsl:FIELD_XCONF :=
    <collection xmlns="http://exist-db.org/collection-config/1.0">
        <index>
            <lucene>
                <analyzer class="org.apache.lucene.analysis.standard.StandardAnalyzer"/>
                <text qname="rsl-div">
                    <field name="rsl-head" expression="head"/>
                </text>
            </lucene>
        </index>
    </collection>;

declare
    %test:setUp
function rsl:setUp() {
    let $_ := (
        xmldb:create-collection("/db/system", "config"),
        xmldb:create-collection("/db/system/config", "db"),
        xmldb:create-collection("/db/system/config/db", $rsl:NAME),
        xmldb:create-collection("/db", $rsl:NAME),
        xmldb:create-collection($rsl:CONFIG, "facet-conflict"),
        xmldb:create-collection($rsl:COLLECTION, "facet-conflict"),
        xmldb:create-collection($rsl:CONFIG, "scope"),
        xmldb:create-collection($rsl:COLLECTION, "scope")
    )
    return (
        rsl:store("facet-conflict/flat", rsl:facet-xconf(false()), <doc><rsl-p>a word</rsl-p></doc>),
        rsl:store("facet-conflict/hierarchical", rsl:facet-xconf(true()), <doc><rsl-p>a word</rsl-p></doc>),
        rsl:store("scope/a", $rsl:SCOPE_XCONF, <doc><rsl-para>an array</rsl-para></doc>),
        rsl:store("scope/b", $rsl:SCOPE_XCONF, <doc><rsl-para>an array</rsl-para></doc>),
        rsl:store("stop", $rsl:STOP_XCONF, <doc><rsl-line>fair, foul</rsl-line></doc>),
        rsl:store("nested", $rsl:NESTED_XCONF,
            <doc><div n="outer"><head>x</head><div n="inner"><head>x</head><p>y</p></div></div></doc>),
        rsl:store("field", $rsl:FIELD_XCONF, <doc><rsl-div><head>Treaty of Paris</head><p>signed</p></rsl-div></doc>)
    )
};

declare
    %test:tearDown
function rsl:tearDown() {
    xmldb:remove($rsl:COLLECTION),
    xmldb:remove($rsl:CONFIG)
};

(:~
 : #6915: a query that asks for no facets is not affected by how the collections configure their facets.
 :)
declare
    %test:assertEquals(2)
function rsl:facet-conflict-without-facets() {
    count(collection($rsl:COLLECTION || "/facet-conflict")//rsl-p[ft:query(., "word")])
};

(:~
 : #6917: ft:search-scope counts each hit's facets once, also over two collections with a configuration each.
 :)
declare
    %test:assertEquals(2, 2, 2)
function rsl:search-scope-facets-over-two-configurations() {
    let $r := ft:search-scope($rsl:COLLECTION || "/scope", "rsl-content:array", map { "facets": "rsl-scope-kind" })
    return ($r?total, array:size($r?hits), $r?facets?rsl-scope-kind?para)
};

(:~
 : #6919: with phrase-as-near, a phrase whose stopword the analyzer drops still matches a text without it, as the
 : phrase does without the option and as <near> does.
 :)
declare
    %test:assertEquals(1, 1, 1)
function rsl:phrase-as-near-stopword-gap-is-not-a-minimum() {
    let $lines := collection($rsl:COLLECTION || "/stop")//rsl-line
    return (
        count($lines[ft:query(., '"fair is foul"~1')]),
        count($lines[ft:query(., <query><near slop="1">fair is foul</near></query>)]),
        count($lines[ft:query(., '"fair is foul"~1', map { "phrase-as-near": "yes" })])
    )
};

(:~
 : A predicate whose descendant step ft:query preselects keeps every nested context node of its hits.
 : Fails on develop already ([inner] for the last two); #6926 turns the last one into [outer], as its copies
 : snapshot the context before the inner div is added to it.
 :)
declare
    %test:assertEquals("[outer inner]", "[outer inner]", "[outer inner]", "[outer inner]", "[outer inner]")
function rsl:nested-context-nodes-with-matches() {
    let $col := collection($rsl:COLLECTION || "/nested")
    let $divs := $col//div[ft:query(., "x")]
    return (
        "[" || string-join($col//div[contains(., "x")][.//p] ! @n, " ") || "]",
        "[" || string-join($col//div[ft:query(., "x")][.//p] ! @n, " ") || "]",
        "[" || string-join($divs[.//p] ! @n, " ") || "]",
        "[" || string-join($col//div[contains(., "x")][.//p[ft:query(., "y")]] ! @n, " ") || "]",
        "[" || string-join($col//div[ft:query(., "x")][.//p[ft:query(., "y")]] ! @n, " ") || "]"
    )
};

(:~
 : #6916: a query that does not name the field, with the field listed in the fields option, highlights the field,
 : as before the PR.
 :)
declare
    %test:assertEquals("Paris")
function rsl:highlight-field-of-unprefixed-query() {
    for $div in collection($rsl:COLLECTION || "/field")//rsl-div[ft:query(., "paris", map { "fields": "rsl-head" })]
    return ft:highlight-field-matches($div, "rsl-head")//exist:match/string()
};
