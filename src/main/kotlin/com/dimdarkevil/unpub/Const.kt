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
    
    html {
        max-width: 100%;
        overflow-x: hidden;
    }
    
    body {
        max-width: 100%;
        overflow-x: hidden;
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
    
    img {
        max-width: 100%;
    }
    
    table {
        width: 100%;
        border-collapse: collapse;
    }
    
    tr {
        width: 100%;
    }
    
    td {
        border: 1px solid #808080;
        padding: 0.5em;
    }

    th {
        border: 1px solid #808080;
        padding: 1em 0.5em;
    }
    
    ul.no-bullets {
        list-style-type: none;
        padding: 0;
        margin: 0em 1em;
    }
    
    a:link, a:active, a:visited, a:hover {
        color: {linkFontColor};
    }    

    a.link-button {
      background-color: #0095ff;
      border: 1px solid transparent;
      border-radius: 3px;
      box-shadow: rgba(255, 255, 255, .4) 0 1px 0 0 inset;
      box-sizing: border-box;
      color: #ffffff;
      cursor: pointer;
      display: inline-block;
      font-family: "Latin Modern Sans", Barlow, Helvetica, sans-serif;      
      font-size: 12pt;
      font-weight: normal;
      line-height: 1.15385;
      margin: 0 0 0 1em;
      outline: none;
      padding: 8px .8em;
      position: relative;
      text-align: center;
      text-decoration: none;
      user-select: none;
      -webkit-user-select: none;
      touch-action: manipulation;
      vertical-align: baseline;
      white-space: nowrap;
    }
    
    .long-text-input {
        width: 50em;
    }

    .button-7 {
      background-color: #0095ff;
      border: 1px solid transparent;
      border-radius: 3px;
      box-shadow: rgba(255, 255, 255, .4) 0 1px 0 0 inset;
      box-sizing: border-box;
      color: #ffffff;
      cursor: pointer;
      display: inline-block;
      font-family: "Latin Modern Sans", Barlow, Helvetica, sans-serif;      
      font-size: 18pt;
      font-weight: bold;
      line-height: 1.15385;
      margin: 0;
      outline: none;
      padding: 8px .8em;
      position: relative;
      text-align: center;
      text-decoration: none;
      user-select: none;
      -webkit-user-select: none;
      touch-action: manipulation;
      vertical-align: baseline;
      white-space: nowrap;
    }
    
    .button-7:hover,
    .button-7:focus {
      background-color: #07c;
    }
    
    .button-7:focus {
      box-shadow: 0 0 0 4px rgba(0, 149, 255, .15);
    }
    
    .button-7:active {
      background-color: #0064bd;
      box-shadow: none;
    }

    input[type="file"] {
      font-family: "Latin Modern Sans", Barlow, Helvetica, sans-serif;
      font-size: 18pt;
      font-weight: normal;
    }
""".trimIndent()

val htmlTop = """
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>unpub</title>
        <link rel="stylesheet" href="style.css">
      </head>
      <body>       
""".trimIndent()

val htmlBottom = """
      </body>
    </html>        
""".trimIndent()

val uploadForm = """
    <h2>Upload</h2>
        <form method="post" action="/upload" enctype="multipart/form-data" accept="application/epub+zip">
            <input type="file" name="files" multiple>
            <button class="button-7">Upload</button>
        </form>
""".trimIndent()

val editForm = """
    <h2>Edit: {title} - {author}</h2>
    <form action="edit" method="post">
         <ul class="no-bullets">
            <li>
              <label for="title">Title:</label>
              <input type="text" id="title" name="title" class="long-text-input" value="{title}"/>
            </li>
            <li>
              <label for="sortTitle">Sort title:</label>
              <input type="text" id="sortTitle" name="sortTitle" class="long-text-input" value="{sortTitle}"/>
            </li>
            <li>
              <label for="author">Author:</label>
              <input type="text" id="author" name="author" class="long-text-input" value="{author}"/>
            </li>
            <li>
              <label for="publisher">Publisher:</label>
              <input type="text" id="publisher" name="publisher" class="long-text-input" value="{publisher}"/>
            </li>
            <li>
              <label for="date">Publish date:</label>
              <input type="text" id="date" name="date" value="{date}"/>
            </li>
            <li class="button">
              <button class="button-7" type="submit">Save</button>
            </li>
          </ul>
    </form>
    <a href="/book/{id}/edit_style.html">Edit custom style.css</a>
""".trimIndent()

val editStyleForm = """
    <h2>Edit custom style.css: {title} - {author}</h2>
    <form action="edit-style" method="post">
        <textarea id="customStyle" name="customStyle" rows="24" cols="80">{style}</textarea>
        <br>
        <button class="button-7" type="submit">Save</button>
    </form>
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
              <button class="button-7" type="submit">Save</button>
            </li>
          </ul>    
    </form>
""".trimIndent()
