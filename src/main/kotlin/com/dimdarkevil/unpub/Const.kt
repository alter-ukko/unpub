package com.dimdarkevil.unpub

import java.io.File

val HOME = File(System.getProperty("user.home"))

val styles = """
    @font-face {
        font-family: "Latin Modern Sans";
        font-weight: normal;
        font-style: normal;
        src: url('/fonts/latin-modern/lmsans10-regular.otf') format('opentype');
    }
    
    @font-face {
        font-family: "Latin Modern Sans";
        font-weight: normal;
        font-style: italic;
        src: url('/fonts/latin-modern/lmsans10-oblique.otf') format('opentype');
    }
    
    @font-face {
        font-family: "Latin Modern Sans";
        font-weight: bold;
        font-style: normal;
        src: url('/fonts/latin-modern/lmsans10-bold.otf') format('opentype');
    }
    
    @font-face {
        font-family: "Latin Modern Sans";
        font-weight: bold;
        font-style: italic;
        src: url('/fonts/latin-modern/lmsans10-boldoblique.otf') format('opentype');
    }
    
    body {
        font-family: "Latin Modern Sans", Barlow, Helvetica, sans-serif;
        font-size: {fontSize};
        margin: {topMargin} {sideMargin};
        color: {fontColor};
        background: {backgroundColor};
        text-align: {textAlign};
    }
    
    h1 {
        font-size: 20pt;
        font-weight: bold;
    }
    
    h2 {
        font-size: 18pt;
        font-weight: bold;
    }
    
    h3 {
        font-size: 16pt;
        font-weight: bold;
    }

    p {
        margin-bottom: {paragraphSpacing};
    }
    
    ul.no-bullets {
        list-style-type: none;
        padding: 0;
        margin: 0em 1em;
    }
    
    a:link, a:active, a:visited, a:hover {
        color: {linkFontColor};
    }    
""".trimIndent()

val htmlTop = """
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8">
        <title>unpub</title>
        <link rel="stylesheet" href="style.css">
      </head>
      <body>       
""".trimIndent()

val htmlBottom = """
      </body>
    </html>        
""".trimIndent()


val settingsForm = """
    <h2>Settings</h2>
    <form action="/settings" method="post">
         <ul class="no-bullets">
            <li>
              <label for="backgroundColor">Background color:</label>
              <input type="text" id="backgroundColor" name="backgroundColor" value="{backgroundColor}"/>
            </li>
            <li>
              <label for="fontColor">Font color:</label>
              <input type="text" id="fontColor" name="fontColor" value="{fontColor}"/>
            </li>
            <li>
              <label for="fontSize">Font size:</label>
              <input type="text" id="fontSize" name="fontSize" value="{fontSize}"/>
            </li>
            <li>
              <label for="topMargin">Top margin:</label>
              <input type="text" id="topMargin" name="topMargin" value="{topMargin}"/>
            </li>
            <li>
              <label for="sideMargin">Side margin:</label>
              <input type="text" id="sideMargin" name="sideMargin" value="{sideMargin}"/>
            </li>
            <li>
              <label for="paragraphSpacing">Paragraph spacing:</label>
              <input type="text" id="paragraphSpacing" name="paragraphSpacing" value="{paragraphSpacing}"/>
            </li>
            <li>
              <label for="linkFontColor">Link font color:</label>
              <input type="text" id="linkFontColor" name="linkFontColor" value="{linkFontColor}"/>
            </li>
            <li>
              <label for="textAlign">Text align:</label>
              <input type="text" id="textAlign" name="textAlign" value="{textAlign}"/>
            </li>
            <li class="button">
              <button type="submit">Save</button>
            </li>
          </ul>    
    </form>
""".trimIndent()
